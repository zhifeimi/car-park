# Trailhead: the parking-booking design system

One visual system for every park that runs this software, in any country: a community lot, a scenic area, or a national park. A deployment sets itself up with **four build-time variables and the server's `TZ`**. It never forks CSS or code.

- Tokens: [`src/tokens.css`](src/tokens.css) (`--np-*`). This file is the single source of truth.
- Live preview: [`preview.html`](preview.html). Open it in a browser and switch biome presets.
- Consumers: `apps/mobile/src/styles/index.css` maps the tokens onto Vant, and `apps/web/src/styles/index.css` maps them onto Element Plus and the admin console's `--cp-*` names.

## Principles

1. **Readable outdoors.** Visitors book on a phone at a trailhead in full sun, often one-handed with a weak signal. Body text is 15px with no thin weights. Tap targets are at least 48px. Every text pairing passes WCAG AA, and body ink passes AAA (table below).
2. **Look like park signage, not SaaS.** Visitors already know the dark routed-wood signboard with cream lettering. It becomes the page header, the admin sidebar and the login screen. Surfaces are flat, with 1px lines instead of shadows.
3. **Each park changes one thing.** A park picks a biome preset, which swaps six brand tokens. Status colors, neutrals, type and spacing are shared by every deployment, so gate staff and admins read every park the same way.
4. **Use system fonts only.** Google Fonts is blocked in mainland China, and a CJK, Thai or Arabic web font costs megabytes on a trailhead connection. The generic stack names no script-specific face. Both apps keep `<html lang>` in sync with the chosen language, and the browser uses it to pick the right Japanese, Korean, Thai, Cyrillic or Arabic fallback. Only `zh-CN` names its faces (PingFang SC → Hiragino Sans GB → Microsoft YaHei), so Chinese text never borrows Japanese glyph forms.
5. **Never show status by color alone.** Every status has a text label (`预约成功` / `已取消`), and the pass header shows the label as well as the color.
6. **Make the pass look like a permit.** The success page is a paper permit with a status band, the park name, the QR, a tear line and the number plate in an outlined plate box. Staff check it from a car window in a couple of seconds.

## Tokens

### Brand (varies per park)

| Token | forest (default) | canyon | glacier | coast | Used for |
|---|---|---|---|---|---|
| `--np-brand` | `#2f5d3a` | `#7b4a2d` | `#245c7a` | `#1e6b66` | Primary buttons, selection, links |
| `--np-brand-strong` | `#244a2e` | `#623a23` | `#1b4a63` | `#175651` | Hover and pressed states |
| `--np-brand-soft` | `#e6eee7` | `#f3eae3` | `#e3eef3` | `#e1f0ee` | Selected tiles, availability panel, table hover |
| `--np-sign` | `#1f3527` | `#3b271b` | `#17323f` | `#133b38` | Signboard header, admin sidebar, login screen |
| `--np-on-sign` | `#f3eedf` | same | same | same | Text on the signboard |
| `--np-on-sign-muted` | `#c9c2ae` | same | same | same | Secondary text on the signboard |

Which preset fits which park:
- **forest**: woodland and mountain parks such as Wuyishan, Giant Panda, Great Smoky Mountains or the Black Forest, and community green space.
- **canyon**: red-rock, desert, loess and grassland parks such as Grand Canyon or Uluru-Kaṯa Tjuṯa. Its sign brown is the classic park-sign brown.
- **glacier**: plateau, lake, glacier and wetland parks such as Sanjiangyuan, Jasper or Torres del Paine.
- **coast**: tropical, rainforest, island, fjord and coastal parks such as Hainan Tropical Rainforest, Komodo or Fiordland.

### Status (shared by every park)

| State | Foreground | Soft background | Contrast | Meaning |
|---|---|---|---|---|
| success | `#276b43` | `#e5f1e8` | 5.5:1 | 预约成功, lots available |
| warning | `#8a5a00` | `#fbf0d9` | 5.2:1 | Caution, form hints |
| danger | `#b42318` | `#fbe9e7` | 5.6:1 | Lot full, errors, cancel action |
| neutral | `#5f635a` | `#eceee9` | 5.3:1 | 已取消, inactive |

### Ground and ink

The neutrals are lichen grey, tinted slightly green rather than a dead mid-grey or a cream.

| Token | Value | On white | On paper |
|---|---|---|---|
| `--np-paper` (page) | `#f1f3ef` | n/a | n/a |
| `--np-surface` (cards) | `#fff` | n/a | n/a |
| `--np-sunken` (tracks, keyboard) | `#e4e8e1` | n/a | n/a |
| `--np-line` | `#d9ddd5` | n/a | n/a |
| `--np-ink` | `#1b201c` | 16.5:1 | 14.8:1 |
| `--np-ink-2` | `#454b45` | 9.0:1 | 8.0:1 |
| `--np-ink-3` | `#646a62` | 5.6:1 | 5.0:1 |

White on `--np-brand` measures forest 7.6, canyon 7.3, glacier 7.3 and coast 6.3. Cream on `--np-sign` is at least 10.6:1 in every preset.

### Type, space, shape

| Group | Tokens |
|---|---|
| Size | `--np-text-xs` 12, `-sm` 13, `-md` 15 (body), `-lg` 17, `-xl` 22 (titles), `-2xl` 30 (availability count) |
| Space (4px grid) | `--np-space-1…6` = 4, 8, 12, 16, 24, 32 |
| Radius | `-sm` 4 (plate box), `-md` 8 (buttons, tiles), `-lg` 12 (cards) |
| Other | `--np-shadow` (admin cards only), `--np-tap` 48px |

Numbers that line up (counts, dates, phone numbers, plates) use `font-variant-numeric: tabular-nums`. Plates get `letter-spacing: 0.08em`.

## Set up a park

Set the variables at build time. They are all optional, and with none set the build is the mainland-China H5 that is already deployed.

```bash
# A park in New Zealand
VITE_REGION=intl \
VITE_SITE_NAME='Fiordland National Park' \
VITE_PARK_THEME=coast \
./scripts/build.sh

# A park in mainland China
VITE_SITE_NAME=武夷山国家公园 VITE_SITE_NAME_EN='Wuyishan National Park' VITE_PARK_THEME=forest ./scripts/build.sh
```

| Variable | Effect |
|---|---|
| `VITE_REGION` | `cn` (default): province plate keyboard and strict mainland plate and 11-digit mobile formats. `intl`: free-text plate in any script and a phone number with country code (E.164) |
| `VITE_SITE_NAME` | Eyebrow above the H5 titles, the admin login heading, and the browser tab titles |
| `VITE_SITE_NAME_EN` | Site name shown when the visitor picks English. Falls back to `VITE_SITE_NAME` |
| `VITE_PARK_THEME` | `forest` \| `canyon` \| `glacier` \| `coast`. Sets `<html data-park>` |
| `TZ` (backend process) | The park's clock, for example `TZ=Pacific/Auckland`. "Today" for booking checks and audit timestamps follow it. Defaults to the host zone, and Jackson defaults to `Asia/Shanghai` |

The variables never change the API contract, the gate QR payload (`{车牌号,手机号,停车场,日期}`, whose keys stay Chinese because gate scanners parse them), or the mobile `/mobile/` + `/prod-api` layout.

**Adding a preset** means adding one `:root[data-park="…"]` block to `tokens.css` with four hex values (`brand`, `brand-strong`, `brand-soft`, `sign`). Keep white on `brand` at 4.5:1 or higher and cream on `sign` at 7:1 or higher. The preview page shows both ratios live.

## Worldwide

**Plates and phones.** The server accepts any country's plate: letters of any script plus digits, with single inner spaces, hyphens or middle dots, 2 to 16 characters. It accepts a mainland mobile or an E.164 number. That character class is also the field's XSS guard (ADR-005). The H5 decides how strict to be through `VITE_REGION`. In `intl` mode it upper-cases the plate, collapses spaces, and strips spaces and punctuation from the phone number before it validates and submits. The normalized phone is also the key that finds the visitor's latest booking, so this step matters. The same sample plates (UK, DE, US, BR, JP, KR, TH, RU, CN) are pinned in `ParkingDtosTest` and `packages/utils/src/validation.test.ts`.

**Languages.** The first visit follows the phone's language (`navigator.languages`) and falls back to English. A visitor's own choice is remembered. Both apps currently ship English and Simplified Chinese. To add a language:
1. Copy `locales/en.js` to `locales/<tag>.js` in each app and translate it, including `common.langShort` (H5 switcher) or `common.languageName` (admin dropdown).
2. Register it in that app's `locales/index.js`: `messages`, plus `vantLocales` (H5) or `elLocales` (admin).
3. For the H5, add the Vant file (for example `vant/es/locale/lang/ja-JP`) to `apps/mobile/deno.json` and `apps/mobile/vite.config.js`, next to the existing `en-US` entries.

**Server messages.** The backend answers in Chinese, and those strings are frozen with the API contract. Both apps translate them through `localizeServerMessage` in `packages/utils`, so a visitor using English never sees a Chinese error toast. Any locale other than `zh-CN` gets English. `server-messages.test.ts` scans the Java sources and fails when a new message has no translation.

**Text length.** English and most other languages run longer than Chinese. The H5 widens Vant's label column (`--van-field-label-width: 7.5em`) and toast, and stops Vant from breaking toasts in the middle of Latin words.

## Rules for components

- Style through `--np-*` tokens, or through the `--van-*` / `--el-*` / `--cp-*` aliases mapped from them. Never write a literal hex value in a view. The one exception is the fallback inside `var(--np-x, #…)` in `packages/ui` components, which keeps them usable without the token file.
- Don't use `color-mix()` in the public H5, because older Android WebViews inside WeChat lack it. The admin console uses it to build Element Plus's tint ramp.
- Destructive actions use plain danger buttons, never brand-primary ones.
- Keep one accent per screen. The four admin dashboard counters share the brand color because they are the same kind of number.

## Deliberately not done

- **Dark mode.** Neither UI library is wired for it yet, and the outdoor booking flow gains little from it. When you add it, redefine only the ground, ink and soft tokens under a `[data-theme="dark"]` block.
- **Right-to-left languages.** Arabic or Hebrew needs `dir="rtl"`, logical CSS properties in place of the remaining `left`/`right` rules, and an RTL-aware Vant setup. Do it together with the first RTL locale.
- **More translations.** Only English and Simplified Chinese ship. Machine-translating languages that no park has reviewed would put wrong rules in front of visitors.
- **Per-country plate rules.** `intl` mode checks characters and length, not each country's exact format. Gate staff compare the plate with the car anyway.
- **Per-park notice text and paid bookings.** The booking rules are generic and say booking is free, which is true because this system takes no payment. A park with its own rules needs them moved into a park field.
