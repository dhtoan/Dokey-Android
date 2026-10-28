# Dokey cho Android

Bàn phím tiếng Việt cho Android, gõ Telex và VNI. Đây là bản Android của Dokey (macOS/Windows).

> **Trạng thái: v0.1, bản thử nghiệm. Chưa thử trên điện thoại thật.**
> CI chỉ chứng minh rằng bộ gõ qua được unit test và APK build được. Phần bàn phím (vẽ phím, chạm, rung,
> theme, chế độ một tay, cài đặt) chưa được ai cài và dùng thử. Hãy coi mọi thứ ngoài bộ gõ là chưa kiểm chứng.

## Tính năng

**Gõ tiếng Việt**
- Telex và VNI. Bỏ dấu kiểu mới (hoà, thuý) hoặc kiểu cũ (hòa, thúy).
- Gõ dấu ở bất kỳ đâu trong từ (`tieesng`, `tieengs` đều ra *tiếng*); dấu luôn nằm đúng chỗ.
- Gõ lặp để bỏ dấu: `ass` → *as*, `aaa` → *aa*, `ww` → *w*. Telex: `w` đứng riêng → *ư*.
- Tự giữ nguyên từ không phải tiếng Việt: *class*, *google*, *Windows*, *user* (tắt được).
- Nút VI/EN trên thanh công cụ và trên bàn phím. Ô mật khẩu, e-mail, URL và ô số luôn gõ không dấu.

**Bàn phím**
- QWERTY, trang số/ký hiệu (có ₫), trang ký hiệu thứ hai, bàn phím số cho ô số/điện thoại.
- Giữ phím: hàng trên ra số, `a` → â ă, `e` → ê, `o` → ô ơ, `u` → ư, `d` → đ, dấu câu phụ.
- Phóng to phím khi nhấn; rung (chỉnh độ mạnh); âm thanh gõ.
- Shift một lần / khoá hoa (nhấn hai lần); tự viết hoa đầu câu; hai lần dấu cách → dấu chấm.
- Vuốt phím cách để di con trỏ; giữ phím cách để đổi bàn phím; giữ Xoá để xoá liên tục.
- Phím Enter đổi biểu tượng theo ô nhập (tìm kiếm, gửi, tiếp, xong).
- Chế độ một tay (trái/phải), ba mức chiều cao phím, tuỳ chọn viền phím, hàng số riêng.

**Giao diện**: 12 theme – Material You (màu theo hình nền, Android 12+), Theo hệ thống, Sáng, Tối,
Đen AMOLED, Đại dương, Hoàng hôn, Hoa anh đào, Rừng thông, Oải hương, Cà phê sữa, Đêm sao.
Màn hình cài đặt có ảnh xem trước từng theme và ô gõ thử.

Chưa có: gợi ý từ, gõ vuốt, emoji, bộ nhớ tạm, giọng nói. Xem [docs/research.md](docs/research.md).

## Cài đặt bản thử

1. Vào **Releases** → bản pre-release mới nhất → tải file `dokey-android-*-debug.apk`.
   (Bản mới có thể tạo bằng workflow **Pre-release** trong tab Actions.)
2. Cài file APK (cần cho phép cài từ nguồn không xác định).
3. Mở ứng dụng Dokey → bật Dokey → chọn Dokey làm bàn phím.

Bản debug được ký bằng khoá tạm của mỗi lần build, nên muốn cài bản mới hơn phải gỡ bản cũ trước.

## Cấu trúc

- `engine/` – bộ gõ tiếng Việt, Kotlin thuần (không phụ thuộc Android), có unit test.
  Viết mới từ quy tắc chính tả tiếng Việt công khai; không dựa trên mã nguồn bộ gõ nào khác.
- `app/` – `InputMethodService` (`ime/DokeyIme.kt`), bàn phím vẽ bằng Canvas (`ime/KeyboardView.kt`),
  màn hình cài đặt bằng Jetpack Compose (`settings/SettingsActivity.kt`).

Bàn phím dùng View tự vẽ thay vì Compose để mở nhanh và nhẹ trên máy yếu; Compose chỉ dùng cho màn hình cài đặt.
Từ đang gõ được hiển thị dạng *composing text* (có gạch chân trong nhiều ứng dụng) và được chốt khi gõ dấu cách
hoặc dấu câu.

## Build

Máy Mac hiện tại không có Android SDK, nên APK được build trên GitHub Actions
(`.github/workflows/android.yml`): chạy `gradle :engine:test` rồi `gradle :app:assembleDebug`
với JDK 17 và Gradle 8.9. Muốn build trên máy có Android SDK:

```bash
gradle :engine:test :app:assembleDebug
```

## Giấy phép

Chưa chọn giấy phép; mọi quyền thuộc về tác giả.
