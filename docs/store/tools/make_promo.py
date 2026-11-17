"""스토어 홍보 이미지(1080x1920) 합성. 사용: python3 docs/store/tools/make_promo.py
원본 화면: 한국어 docs/screens, 영어 docs/store/src-en (./gradlew testDebugUnitTest --tests '*ScreenshotTest' / '*EnglishScreenshotTest')."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os
ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
W, H = 1080, 1920
KO_FONT = "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"
EN_BOLD = "/usr/share/fonts/opentype/inter/Inter-Bold.otf"
EN_REG = "/usr/share/fonts/opentype/inter/Inter-Medium.otf"

SHOTS = ["57_home_expert_open_devil_locked", "37_hint_naked_step2_eliminated_digits", "49_lesson_xwing_demo2",
         "53_training_rank_premium_hook", "30_complete_send_challenge", "20_game_green_theme", "15_premium"]
TEXT = {  # 언어 -> [(제목, 부제)] x 7 (SHOTS 순서)
    "ko": [("쉬움부터 악마까지", "5단계 난이도, 오프라인으로 언제든"), ("막히면 힌트가 이유까지", "정답이 아니라 푸는 방법을 알려줘요"),
           ("눈으로 배우는 X-윙", "패턴이 보이면 어려운 퍼즐도 풀려요"), ("나도 초고수가 될 수 있다", "기본부터 18가지 기법까지 단계별 트레이닝"),
           ("내가 푼 퍼즐로 친구와 대결", "서버 없이 코드 하나로 도전장 보내기"), ("점수 · 이펙트 · 테마", "한 줄 완성할 때마다 짜릿하게"),
           ("한 번 결제로 평생 프리미엄", "구독 없음 · 광고 없음")],
    "en": [("From Easy to Devil", "5 levels, fully offline"), ("Hints that teach", "See why, not just the answer"),
           ("Learn X-Wing by sight", "Spot the pattern, solve the hard ones"), ("Become a grandmaster", "18 techniques, step by step"),
           ("Challenge your friends", "Send a puzzle with one code"), ("Score, effects, themes", "Every line feels rewarding"),
           ("Pay once, keep forever", "No subscription · No ads")],
    "ja": [("やさしいから悪魔級まで", "5段階の難易度、オフラインで遊べる"), ("詰まったらヒントが理由まで", "答えだけでなく解き方を教えます"),
           ("目で学ぶ X-ウィング", "パターンが見えれば難問も解ける"), ("あなたも超上級者になれる", "基礎から18のテクニックまで段階トレーニング"),
           ("解いた問題で友達と対戦", "サーバー不要、コード1つで挑戦状を送れる"), ("スコア・演出・テーマ", "ラインが揃うたびにワクワク"),
           ("一度の購入でずっとプレミアム", "サブスクなし・広告なし")],
    "es": [("De Fácil a Diabólico", "5 niveles, sin conexión"), ("Pistas que enseñan", "Entiende el porqué, no solo la respuesta"),
           ("Aprende el X-Wing a la vista", "Ve el patrón y resuelve los difíciles"), ("Hazte gran maestro", "18 técnicas, paso a paso"),
           ("Reta a tus amigos", "Envía un sudoku con un solo código"), ("Puntos, efectos y temas", "Cada línea completada se siente genial"),
           ("Paga una vez, tuyo para siempre", "Sin suscripción · Sin anuncios")],
    "pt": [("Do Fácil ao Diabólico", "5 níveis, totalmente offline"), ("Dicas que ensinam", "Veja o porquê, não só a resposta"),
           ("Aprenda o X-Wing na prática", "Veja o padrão e resolva os difíceis"), ("Vire um grão-mestre", "18 técnicas, passo a passo"),
           ("Desafie seus amigos", "Envie um sudoku com um único código"), ("Pontos, efeitos e temas", "Cada linha completa é uma delícia"),
           ("Pague uma vez, use para sempre", "Sem assinatura · Sem anúncios")],
}
CJK = ("ko", "ja")

def gradient():
    top, bot = (52, 84, 214), (28, 40, 140)
    im = Image.new("RGB", (W, H))
    px = im.load()
    for y in range(H):
        t = y / (H - 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        for x in range(W):
            px[x, y] = c
    return im

BG = gradient()

def text_c(d, y, s, font, fill, ko):
    sw = 2 if ko else 0  # 한글 폰트는 굵기가 없어 외곽선으로 보강
    w = d.textlength(s, font=font)
    d.text(((W - w) / 2, y), s, font=font, fill=fill, stroke_width=sw, stroke_fill=fill)

def make(lang, idx, shot, head, sub):
    ko = lang in CJK
    src = os.path.join(ROOT, "screens" if lang == "ko" else f"store/src-{lang}", shot + ".png")
    img = BG.copy()
    d = ImageDraw.Draw(img)
    size = 84 if ko else 80
    fh = ImageFont.truetype(KO_FONT if ko else EN_BOLD, size)
    while d.textlength(head, font=fh) > 940:  # 긴 제목은 글자 크기를 줄여 좌우 여백 확보
        size -= 2
        fh = ImageFont.truetype(KO_FONT if ko else EN_BOLD, size)
    fs = ImageFont.truetype(KO_FONT if ko else EN_REG, 40 if ko else 40)
    text_c(d, 120, head, fh, "white", ko)
    text_c(d, 245, sub, fs, (214, 224, 255), False)
    scr = Image.open(src).convert("RGB")
    sw = 680
    sh = int(scr.height * sw / scr.width)
    scr = scr.resize((sw, sh), Image.LANCZOS)
    bez = 16
    fw, fh2 = sw + 2 * bez, min(sh, H - 360 - 2 * bez) + 2 * bez
    x0, y0 = (W - fw) // 2, 360
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x0, y0 + 14, x0 + fw, y0 + fh2 + 14), 70, fill=(0, 0, 0, 120))
    img.paste(Image.new("RGB", (W, H), (0, 0, 0)), (0, 0), shadow.filter(ImageFilter.GaussianBlur(24)))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x0, y0, x0 + fw, y0 + fh2), 70, fill=(18, 20, 30))
    crop = scr.crop((0, 0, sw, fh2 - 2 * bez))
    mask = Image.new("L", crop.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, crop.width, crop.height), 56, fill=255)
    # 아래쪽이 잘리는 경우 하단은 프레임 밖으로 자연스럽게 이어지도록 사각 처리
    if sh > fh2 - 2 * bez:
        ImageDraw.Draw(mask).rectangle((0, crop.height - 60, crop.width, crop.height), fill=255)
    img.paste(crop, (x0 + bez, y0 + bez), mask)
    out = os.path.join(ROOT, "store", f"promo-{lang}")
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, f"{idx:02d}.png"), optimize=True)

for lang, rows in TEXT.items():
    for i, (shot, (head, sub)) in enumerate(zip(SHOTS, rows), 1):
        make(lang, i, shot, head, sub)
print("done")
