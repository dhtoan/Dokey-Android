<div align="center">

# Dokey cho Android

**Bàn phím tiếng Việt gọn, nhanh, đẹp – gõ Telex và VNI, chạy hoàn toàn trên máy.**

Bản Android của Dokey (macOS/Windows) · Kotlin · Android 7.0 trở lên

[Tải bản thử v0.1.0](https://github.com/dhtoan/Dokey-Android/releases/tag/v0.1.0) ·
[Tính năng](#tính-năng) · [Theme](#12-theme) · [Cài đặt](#cài-bản-thử) · [Trạng thái](#trạng-thái)

<img src="docs/mockups/theme-light.svg" width="360" alt="Dokey, theme Sáng">&nbsp;&nbsp;
<img src="docs/mockups/theme-sunset.svg" width="360" alt="Dokey, theme Hoàng hôn">

<sub>Ảnh minh hoạ được vẽ từ đúng bảng màu và bố cục phím trong mã nguồn
(<code>scripts/mockups.py</code>), không phải ảnh chụp từ điện thoại.</sub>

</div>

---

## Trạng thái

> **v0.1.0 – bản thử nghiệm, chưa thử trên điện thoại thật.**
>
> | Phần | Đã kiểm chứng bằng |
> |---|---|
> | Bộ gõ Telex/VNI | Unit test (`engine/src/test`), chạy xanh trên CI |
> | Build APK | Đã build thành công, APK gắn ở bản pre-release v0.1.0 |
> | Màn hình cài đặt | Mở và hiển thị được trên trình giả lập Android 14 (một lần chạy CI) |
> | Bàn phím: vẽ phím, chạm, rung, theme, một tay | **Chưa kiểm chứng** – chưa ai cài và gõ thử |

## Tính năng

### Gõ tiếng Việt thông minh
- **Telex và VNI**, chọn bỏ dấu kiểu mới (*hoà, thuý*) hoặc kiểu cũ (*hòa, thúy*).
- **Gõ dấu ở đâu cũng được**: `tieesng` hay `tieengs` đều ra *tiếng*; dấu luôn nằm đúng chỗ theo chính tả.
- **Gõ lặp để bỏ dấu**: `ass` → *as*, `aaa` → *aa*, `ww` → *w*. Telex: `w` đứng riêng → *ư*.
- **Giữ nguyên từ tiếng Anh**: *class, google, Windows, user* không bị thêm dấu (tắt được).
- Ô mật khẩu, e-mail, URL và ô số luôn gõ không dấu.

| Bạn gõ (Telex) | Ra | Bạn gõ (VNI) | Ra |
|---|---|---|---|
| `vieetj` | việt | `vie65t` | việt |
| `nguwowif` | người | `nguo7i2` | người |
| `dduwowngf` | đường | `d9uo7ng2` | đường |
| `hoawcj` | hoặc | `hoa2` | hoà |
| `google` | google | `Covid19` | Covid19 |

### Bàn phím
- QWERTY, trang số – ký hiệu (có **₫**), trang ký hiệu thứ hai, bàn phím số cho ô số/điện thoại.
- **Giữ phím** để chọn ký tự phụ: hàng trên → số; `a` → â ă, `e` → ê, `o` → ô ơ, `u` → ư, `d` → đ.
- **Phóng to phím khi nhấn**, **rung** chỉnh được độ mạnh, âm thanh gõ.
- Shift một lần / khoá chữ hoa (chạm hai lần), tự viết hoa đầu câu, chạm dấu cách hai lần → dấu chấm.
- **Vuốt phím cách để di con trỏ**, giữ phím cách để đổi bàn phím, giữ Xoá để xoá liên tục.
- Phím Enter đổi biểu tượng theo ô nhập: tìm kiếm, gửi, tiếp, xong.
- **Chế độ một tay** (trái/phải), 3 mức chiều cao phím, bật/tắt viền phím, hàng số riêng (nên bật khi gõ VNI).

<div align="center">
<img src="docs/mockups/one-hand.svg" width="360" alt="Chế độ một tay">
<br><sub>Chế độ một tay: bàn phím thu về một bên, có nút đổi bên và nút trở lại cỡ đầy đủ.</sub>
</div>

## 12 theme

**Material You** (màu lấy theo hình nền, Android 12+), **Theo hệ thống** (tự đổi sáng/tối), và 10 theme cố định:

<table>
<tr>
<td align="center"><img src="docs/mockups/theme-light.svg" width="250"><br>Sáng</td>
<td align="center"><img src="docs/mockups/theme-dark.svg" width="250"><br>Tối</td>
<td align="center"><img src="docs/mockups/theme-amoled.svg" width="250"><br>Đen AMOLED</td>
</tr>
<tr>
<td align="center"><img src="docs/mockups/theme-ocean.svg" width="250"><br>Đại dương</td>
<td align="center"><img src="docs/mockups/theme-sunset.svg" width="250"><br>Hoàng hôn</td>
<td align="center"><img src="docs/mockups/theme-midnight.svg" width="250"><br>Đêm sao</td>
</tr>
<tr>
<td align="center"><img src="docs/mockups/theme-sakura.svg" width="250"><br>Hoa anh đào</td>
<td align="center"><img src="docs/mockups/theme-lavender.svg" width="250"><br>Oải hương</td>
<td align="center"><img src="docs/mockups/theme-coffee.svg" width="250"><br>Cà phê sữa</td>
</tr>
<tr>
<td align="center"><img src="docs/mockups/theme-forest.svg" width="250"><br>Rừng thông</td>
<td></td><td></td>
</tr>
</table>

Màn hình cài đặt có hình xem trước cho từng theme và ô để gõ thử ngay.

## Cài bản thử

1. Mở [bản pre-release v0.1.0](https://github.com/dhtoan/Dokey-Android/releases/tag/v0.1.0) và tải
   `dokey-android-v0.1.0-debug.apk`.
2. Cài file APK (Android sẽ hỏi cho phép cài từ nguồn không xác định).
3. Mở ứng dụng **Dokey** → **Bật Dokey** → **Chọn bàn phím**.

Bản debug được ký bằng khoá tạm, nên muốn cài một bản build khác phải gỡ bản cũ trước.

## Sắp tới

Gợi ý từ · gõ vuốt · emoji · bộ nhớ tạm · nhập bằng giọng nói · theme ảnh nền tuỳ chọn.
Ý tưởng lấy từ khảo sát Gboard, SwiftKey, Samsung Keyboard và Laban Key: [docs/research.md](docs/research.md).

## Cấu trúc mã nguồn

```
engine/   Bộ gõ tiếng Việt – Kotlin thuần, không phụ thuộc Android, có unit test
app/      InputMethodService, bàn phím vẽ bằng Canvas, màn hình cài đặt Jetpack Compose
docs/     Khảo sát, ảnh minh hoạ
scripts/  mockups.py (vẽ ảnh minh hoạ), screenshots.sh (chụp ảnh trên trình giả lập)
```

- **Bộ gõ viết mới** từ quy tắc chính tả tiếng Việt công khai, không dựa trên mã nguồn bộ gõ nào khác.
  Vì là Kotlin thuần nên sau này có thể dùng lại cho iOS bằng Kotlin Multiplatform.
- **Bàn phím là View tự vẽ** (không dùng Compose) để mở nhanh và nhẹ trên máy yếu; Compose chỉ dùng cho cài đặt.
- Từ đang gõ hiển thị dạng *composing text* (có gạch chân trong nhiều ứng dụng) và được chốt khi gõ dấu cách
  hoặc dấu câu.

## Build

Cần JDK 17, Gradle 8.9 và Android SDK (compileSdk 35):

```bash
gradle :engine:test :app:assembleDebug
```

Repo hiện không có workflow GitHub Actions. `scripts/screenshots.sh` chụp ảnh bàn phím trên một trình giả lập
đang chạy (cần bản debug, vì nó dùng màn hình demo chỉ có trong bản debug).

## Giấy phép

Chưa chọn giấy phép; mọi quyền thuộc về tác giả.
