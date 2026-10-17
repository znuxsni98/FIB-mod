#version 150

// 维度裂缝。整套效果逐像素算，只画一个四边形。
// crackCoord 是归一化局部坐标：撕裂边缘 = 单位圆，顶点侧按 2:3 缩放 → 竖直椭圆。
//
// 分两遍画（CrackMode 区分），因为两遍的深度写入要求相反：
//   CrackMode = 0  实心内部：alpha 在轮廓内是 1，这一遍写深度，
//                  否则裂缝不写深度，后面的实体不管在裂缝前面还是后面都会盖上来。
//   CrackMode = 1  边缘/气焰/水波纹/闪电：透明层，不能写深度。
// 两遍用同一个 shader，只是 RenderType 的 WriteMaskStateShard 不同，见 ModShaders。
//
// 三张贴图分工：
//   Sampler0 星空   —— 裂缝内部的底
//   Sampler1 水波纹 —— 打碎扩张环的亮度，让环断成不规则的水光
//   Sampler2 闪电   —— 边框外的细裂缝，位置固定，只有明暗在缓慢变化
//
// 关于极坐标：角度一律走单位向量 dir = vec2(cos(ang), sin(ang))，不要用 ang * 常数
// —— atan 在 ±PI 有接缝，dir 天然周期。但 *贴图* 不要在极坐标里采样：
// 一张有细长笔画的图，按 dir 采样会把每条笔画沿半径抹成一根放射状的针。

uniform sampler2D Sampler0;      // 星空
uniform sampler2D Sampler1;      // 水波纹
uniform sampler2D Sampler2;      // 闪电

uniform float CrackTime;         // tick
uniform float CrackParU;         // 视差偏移
uniform float CrackParV;
uniform float CrackScaleU;       // 星空平面映射系数
uniform float CrackScaleV;
uniform float CrackAlpha;        // 开启动画的整体淡入
uniform float CrackMode;         // 0 = 实心内部，1 = 边缘层

in vec4 vertexColor;
in vec2 crackCoord;

out vec4 fragColor;

const float PI  = 3.14159265359;
const float TAU = 6.28318530718;

// 轮廓扰动的最大幅度。
// 太小（±0.06）看着像个规规矩矩的椭圆，太大（±0.22）会把椭圆糊成团块。
// 这里低频给起伏、高频给"扭曲感"，加权和最大 0.072 * 1.48 ≈ ±0.11，实际同时到顶很少。
const float EDGE_AMP = 0.072;

// ==================== 边框配色 ====================
// 三层叠出"有厚度的实心边"：最外暗紫红雾 → 橙红主体 → 最内白热芯。
// 之前是一层奶油白（1.00, 0.86, 0.72），和星空贴图的紫粉完全不搭，像贴了张白纸。
const vec3 RIM_HOT   = vec3(1.00, 0.92, 0.78);   // 内芯，接近白热
const vec3 RIM_MID   = vec3(1.00, 0.42, 0.14);   // 主体，橙红
const vec3 RIM_OUTER = vec3(0.55, 0.08, 0.24);   // 外雾，暗紫红，和星空的紫过渡

// 边框总厚度（归一化半径）。三层都按它缩放。
const float RIM_W = 0.048;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.03 + vec2(17.3, 9.1);
        a = a * 0.5;
    }
    return v;
}

// 在 [a,b] 之间为 1，两侧用 soft 宽度平滑过渡
float band(float x, float a, float b, float soft) {
    return smoothstep(a - soft, a + soft, x) * (1.0 - smoothstep(b - soft, b + soft, x));
}

void main() {
    vec2 p = crackCoord;
    float r = length(p);
    if (r > 1.80) {
        discard;
    }

    float t = CrackTime;
    float ang = atan(p.y, p.x);
    vec2 dir = vec2(cos(ang), sin(ang));

    // ---- 1) 撕裂轮廓：竖直椭圆 + 慢速的小范围扰动 ----
    // wN  低频起伏（噪声场整体缓慢平移，不是每 tick 重抽）
    // wN2 高频细节，负责"边框有点扭曲"
    // wS  明确向外的行波：波峰每 tick 外移 0.011，一个明暗周期约 5.7 秒
    float wN  = (fbm(dir * 2.60 + vec2(t * 0.0045, -t * 0.0030)) - 0.5) * 2.0;
    float wN2 = (fbm(dir * 6.30 + vec2(-t * 0.0075, t * 0.0055)) - 0.5) * 2.0;
    float agMod = 0.50 + 0.50 * sin(ang * 3.0 + 0.7);
    float wS = sin(r * 5.0 - t * 0.055) * agMod;
    float wob = 1.0 + EDGE_AMP * (0.52 * wN + 0.34 * wN2 + 0.62 * wS);

    // ---- 2) 到轮廓的有符号距离：<0 在裂缝内部，>0 在外 ----
    float d = r - wob;

    // ================= 第一遍：实心内部（写深度） =================
    if (CrackMode < 0.5) {
        float mask = 1.0 - smoothstep(-0.012, 0.020, d);
        if (mask < 0.004) {
            discard;
        }
        vec2 suv = vec2(0.5) + vec2(p.x, -p.y) * vec2(CrackScaleU, CrackScaleV) + vec2(CrackParU, CrackParV);
        vec3 neb = texture(Sampler0, suv).rgb;
        // 越靠外越暗，中心最亮 —— 不然只是一张平铺图，没有"洞"的纵深
        neb *= mix(1.35, 0.55, smoothstep(0.0, 0.95, r));
        fragColor = vec4(clamp(neb, 0.0, 1.0), clamp(mask * CrackAlpha * vertexColor.a, 0.0, 1.0));
        return;
    }

    // ================= 第二遍：边缘层（不写深度） =================
    vec3 col = vec3(0.0);
    float alpha = 0.0;

    // ---- 3) 实心边缘：三层叠出厚度 ----
    // 上一版是一根 exp(-|d| * 50) 的细线，屏幕上两三个像素。
    // 现在 rimCore 是白热芯、rimMain 是橙红主体、rimHaze 是向外散开的暗紫红雾。
    float ad = abs(d);
    float rimCore = 1.0 - smoothstep(RIM_W * 0.28, RIM_W * 0.62, ad);
    float rimMain = 1.0 - smoothstep(RIM_W * 0.55, RIM_W * 1.00, ad);
    float rimHaze = 1.0 - smoothstep(RIM_W * 1.00, RIM_W * 2.60, ad);

    col += RIM_OUTER * rimHaze * 0.75
         + RIM_MID   * rimMain * 1.25
         + RIM_HOT   * rimCore * 0.80;
    alpha = max(alpha, max(rimMain * 0.95, rimHaze * 0.55));

    // ---- 4) 气焰：三层不同宽度，缓慢向外的行波 ----
    float flame = 0.0;
    for (int k = 0; k < 3; k++) {
        float fk = float(k);
        float cells = 13.0 + 11.0 * fk;
        float cell = floor((ang + PI) / TAU * cells);
        float s1 = hash21(vec2(cell, fk * 11.0 + 1.0));
        float s2 = hash21(vec2(cell, fk * 11.0 + 2.0));
        float s3 = hash21(vec2(cell, fk * 11.0 + 3.0));

        float cAng = (cell + 0.5) / cells * TAU - PI;
        float dA = ang - cAng;
        dA = atan(sin(dA), cos(dA));

        float halfW = (TAU / cells) * (0.16 + 0.30 * s2) * (1.0 - 0.20 * fk);
        float len = (0.09 + 0.26 * s1)
            * (0.55 + 0.45 * (0.5 + 0.5 * sin(t * (0.055 + 0.085 * s2) + s1 * 25.0)));
        float t01 = (r - 0.95 * wob) / max(len, 0.001);

        if (t01 > 0.0 && t01 < 1.0 && abs(dA) < halfW) {
            float across = 1.0 - abs(dA) / halfW;
            across = across * across;
            float along = 1.0 - t01;
            // 相位带 -t → 亮区随时间往尖端走，就是"从内向外部扩散"
            float wave = 0.25 + 0.75 * (0.5 + 0.5 * sin(TAU * fract(t01 * 1.1 - t * 0.020 + s1 * 3.0)));
            flame += across * along * wave * (0.50 - 0.13 * fk) * (0.6 + 0.6 * s3);
        }
    }
    flame = min(flame, 1.4);
    col += mix(vec3(1.00, 0.60, 0.28), vec3(0.72, 0.12, 0.10), clamp(1.0 - flame, 0.0, 1.0)) * flame * 0.70;
    alpha = max(alpha, min(flame * 0.70, 1.0));

    // ---- 5) 水波纹：沿半径的连续行波，半径被噪声扭曲 ----
    // 三条 exp(-|r-rad|) 的硬环按 k/3 等相位铺开就是同心圆靶子，改成：
    //   (a) 先把半径坐标按角度扭曲（warpN），环就不是同心椭圆了；
    //   (b) 用 sin^4 的连续行波代替离散环，视野里只看得见一两条波；
    //   (c) 乘上水波贴图，把环打断成一段一段的水光。
    float warpN = (fbm(dir * 2.90 + vec2(t * 0.0052, -t * 0.0035)) - 0.5) * 2.0;
    float rw = r + warpN * 0.15;

    float s = sin(rw * 6.5 - t * 0.030);
    s = s * s * s * s;                                   // 收成细环
    float rippleTex = texture(Sampler1, vec2(0.5) + dir * 0.68 + vec2(t * 0.0035, -t * 0.0055)).r;
    s *= (0.20 + 1.40 * rippleTex);                      // 贴图打断，环上明暗不均
    s *= band(r, 0.70 * wob, 1.60 * wob, 0.32);
    col += vec3(1.00, 0.44, 0.22) * s * 0.55;
    alpha = max(alpha, min(s * 0.60, 1.0));

    // ---- 6) 边框外的闪电裂缝 ----
    // (a) 在平面坐标 p 里采样，细丝才保留自己的形状。
    //     按 dir 采样会把每条笔画沿半径拉成一根针，围着椭圆排成一圈等距的尖刺。
    // (b) 位置固定不动。闪电是"边框外的一块"，不是一群往外跑的东西，
    //     所以这里没有任何 UV 平移，变化全部走明暗（见下面的 breathe）。
    vec2 boltUV = p * 0.28 + vec2(0.5, 0.45);
    float boltTex = texture(Sampler2, boltUV).r;
    float bolt = smoothstep(0.30, 0.76, boltTex);

    // 明灭：一块低频噪声场在极缓慢地漂（0.0075 UV/tick，约 22 秒走一格），
    // 每根闪电跟着自己那一块的噪声渐亮渐暗，不会整片一起闪。
    float puls = fbm(p * 0.85 + vec2(t * 0.0075, -t * 0.0055));
    float breathe = smoothstep(0.30, 0.60, puls);
    // 再叠一层很慢的全局呼吸（周期约 29 秒），让亮的那几根也有一点点起伏
    breathe *= 0.45 + 0.55 * (0.5 + 0.5 * sin(t * 0.018 + ang));
    bolt *= breathe * band(r, 0.90 * wob, 1.34 * wob, 0.10);

    col += vec3(1.00, 0.90, 0.78) * bolt * 1.15;
    alpha = max(alpha, min(bolt * 1.10, 1.0));

    // ---- 7) 收尾 ----
    // 这里原来是 exp(-max(d,0)*3.2) 的外光晕，衰减太慢，在裂缝外面铺了一大片暗淡的雾。
    // 已经删掉：现在超出气焰/水波/闪电的范围就完全透明。
    float outAlpha = clamp(alpha * CrackAlpha * vertexColor.a, 0.0, 1.0);
    if (outAlpha < 0.004) {
        discard;
    }
    fragColor = vec4(clamp(col, 0.0, 1.0), outAlpha);
}
