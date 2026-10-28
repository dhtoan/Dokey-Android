# Khảo sát bàn phím Android (10/2026)

Chỉ lấy ý tưởng về trải nghiệm. Không dùng code hay hình ảnh của ứng dụng nào.

| Ứng dụng | Điểm đáng học | Điểm yếu (theo đánh giá người dùng) |
|---|---|---|
| **Gboard** | Thanh công cụ gọn phía trên, giữ phím hàng trên ra số, vuốt phím cách để di con trỏ, theme Material You theo hình nền, tùy chọn viền phím, chế độ một tay, chiều cao phím | Gõ tiếng Việt kém linh hoạt (chữ ư phải gõ uw), đoán từ tiếng Việt yếu |
| **SwiftKey** | Nhiều theme, kể cả theme ảnh/gradient; bộ nhớ tạm mạnh; thống kê gõ | Tài khoản cũ bị bỏ từ 31/05/2026 |
| **Samsung Keyboard** | Tùy biến sâu (kích thước, độ trong suốt), xử lý trên máy | Chỉ có trên máy Galaxy |
| **Laban Key** | Telex + VNI, **bỏ dấu ở bất kỳ đâu trong từ**, tự nhận ra từ tiếng Anh để không thêm dấu | Một số lỗi tương thích với Facebook/Zalo khi có emoji |

## Ý tưởng đưa vào Dokey

Đã làm trong v0.1:
- Gõ dấu ở bất kỳ vị trí nào trong từ; dấu luôn được đặt lại đúng chỗ theo chính tả.
- Tự giữ nguyên từ không phải tiếng Việt (class, google, Windows…).
- Thanh công cụ: nút VI/EN, chế độ một tay, cài đặt, ẩn bàn phím.
- Giữ phím hàng trên ra số; giữ nguyên âm ra â ă ê ô ơ ư, giữ d ra đ.
- Vuốt phím cách để di con trỏ; giữ phím cách để đổi bàn phím; giữ Xoá để xoá liên tục.
- Phóng to phím khi nhấn, rung có chỉnh độ mạnh, âm thanh gõ.
- 12 theme: Material You (Android 12+), theo hệ thống, sáng, tối, AMOLED và 7 theme màu/gradient.
- Chế độ một tay (trái/phải), ba mức chiều cao phím, tùy chọn viền phím, hàng số riêng.

Để sau:
- Thanh gợi ý từ (cần từ điển tiếng Việt có giấy phép phù hợp).
- Gõ vuốt (swipe typing) – cần mô hình từ điển, tốn công nhất.
- Emoji, bộ nhớ tạm, nhập bằng giọng nói.
- Theme bằng ảnh nền tùy chọn.

Nguồn tham khảo:
- [FPT Shop – Top bàn phím tiếng Việt cho Android](https://fptshop.com.vn/tin-tuc/giai-tri/ban-phim-tieng-viet-cho-android-159092)
- [Laban Key trên APKMirror](https://www.apkmirror.com/apk/zalo-group/laban-key-vietnamese-keyboard/laban-key-vietnamese-keyboard-26-05-01-release/)
- [Tinh tế – đổi sang Gboard](https://tinhte.vn/posts/67296853/)
- [Make Tech Easier – Gboard vs Samsung Keyboard vs SwiftKey](https://maketecheasier.com/gboard-samsung-keyboard-swiftkey)
- [Android Authority – SwiftKey vs Gboard](https://www.androidauthority.com/swiftkey-vs-gboard-3633825/)
