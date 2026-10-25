# KỊCH BẢN KIỂM TRA LUỒNG MỚI (BẢN DỄ HIỂU)

> Chỉ là tài liệu, **không sửa code**. Viết đơn giản, dùng ví dụ quán cafe.

---

## PHẦN 1. NHÂN VIÊN ĐANG "CHẠY LUỒNG CỦA SERVER" LÀ GÌ?

### Ví dụ đời thường
Quán có **1 quầy pha chế** (Server) giữ cuốn sổ ghi đơn **thật**.
Nhân viên phục vụ đứng ở bàn, có một cuốn sổ riêng của mình.

- Khi nhân viên **ghi đơn mới** → chạy sang quầy pha chế báo. ✔ Đúng.
- Khi nhân viên **xem bàn nào có khách, đơn nào đang làm** → lại mở **cuốn sổ riêng của mình**, không hỏi quầy pha chế. ✘ Sai.

Hai cuốn sổ có thể ghi **khác nhau**. Nhân viên thấy bàn trống, nhưng quầy pha chế thì đang làm đơn cho bàn đó.

### Trong code
- Màn hình nhân viên (`FloorPlanView`, `OrdersDialog`) đang đọc thẳng dữ liệu của Server bằng `CafeServerService.getInstance()` (khoảng 16 chỗ).
- Khi Server và nhân viên chạy **trên 2 máy khác nhau**, lệnh này lấy dữ liệu **trên máy nhân viên**, không phải của Server.

### Cách sửa (nói đơn giản)
**Quy tắc duy nhất:** Nhân viên, khách, admin **chỉ được hỏi Server qua mạng (socket)**. Không tự mở sổ riêng.
- Muốn xem bàn → gửi câu hỏi cho Server → Server trả lời.
- Server có thay đổi gì → Server báo cho mọi máy.

> Chưa chắc 100% là đang lệch dữ liệu, vì tôi mới đọc code chứ chưa chạy 2 máy. **Kịch bản A bên dưới sẽ cho bạn biết chắc.**

---

## PHẦN 2. ADMIN CHIA 2 PHẦN: ĐÃ ỔN CHƯA?

**Chưa đủ rõ.** Chia 2 phần là đúng hướng, nhưng phải nói rõ **tab nào thuộc phần nào** và **ai được vào**.

### Hai phần là gì?
| Phần | Giống như | Gồm những gì |
|---|---|---|
| **Quản lý quán** | Việc của người quản lý | Thực đơn & giá, kho, khuyến mãi, nhân sự, báo cáo, xem đơn |
| **Quản lý máy chủ** | Việc của kỹ thuật/chủ quán | Kick thiết bị, duyệt thiết bị xin vào lại, kiểm tra mạng (ping), đổi cổng, tắt server |

### Tab nào vào phần nào
| Tab | Phần |
|---|---|
| 1 Tổng quan, 2 Gọi món & bàn, 3 Đơn hàng | Quản lý quán |
| 5 Thực đơn & giá, 6 Kho, 7 Khuyến mãi | Quản lý quán |
| 8 Nhân sự | Quản lý quán (chỉ **Chủ** được sửa) |
| 9 Báo cáo | Quản lý quán |
| 4 Bếp & Bar | Của quầy pha chế (admin chỉ xem) |
| 11 Hạ tầng & Mạng | Quản lý máy chủ |
| 10 Cài đặt | Chia đôi: thông tin quán → Quản lý quán; cổng mạng, tắt máy → Quản lý máy chủ |

### Ai được vào
| Việc | Quản lý | Chủ |
|---|:-:|:-:|
| Phần "Quản lý quán" | ✔ | ✔ |
| Sửa nhân sự | chỉ xem | ✔ |
| Kick, duyệt thiết bị | ✘ | ✔ (nhập lại mật khẩu) |
| Đổi cổng, tắt server | ✘ | ✔ **chỉ ở máy Server** |

**Lý do:** nếu đổi cổng hoặc tắt server từ xa, toàn bộ máy trong quán mất kết nối.

> Hiện code coi Chủ và Quản lý **giống nhau**. Nên tách ra như bảng trên.

---

## PHẦN 3. CÁC BÀI KIỂM TRA (làm từng bài, tick ✔ nếu đúng)

### Chuẩn bị
- **Máy 1:** mở `ServerMain` (quầy pha chế).
- **Máy 2:** mở `ClientMain` → đăng nhập nhân viên/admin.
- **Máy 3:** mở `ClientMain` → nhập số bàn 05 (khách).
- Chỉ có 1 máy? Mở 3 cửa sổ chạy riêng cũng được.
- Tài khoản: `admin` (Chủ), `manager` (Quản lý), `cashier1` (Thu ngân), `waiter` (Phục vụ), `barista1` (Pha chế).

### Bài A. Có đúng là cả 3 máy thấy cùng một dữ liệu không? *(làm bài này trước)*
| Bước | Làm gì | Phải thấy |
|:-:|---|---|
| 1 | Máy 3 (khách bàn 05) gọi 2 món | Quầy pha chế thấy đơn ngay |
| 2 | Máy 2 (nhân viên) mở sơ đồ bàn | Bàn 05 hiện "Có khách", số tiền đúng |
| 3 | Máy 2 mở Đơn hàng | Thấy đúng đơn đó, cùng mã đơn |
| 4 | Máy 2 đăng nhập admin, mở Tổng quan | Số đơn, doanh thu **khớp** với quầy pha chế |
| 5 | Quầy pha chế bấm "Hoàn thành" | Cả 3 máy đổi trạng thái ngay |

✔ Đạt: mọi số liệu giống nhau trên cả 3 máy.
✘ Không đạt: nếu máy 2 thấy bàn trống hoặc số tiền khác → xác nhận đang lệch dữ liệu → cần sửa theo Phần 1.

### Bài B. Khách
| Làm gì | Phải thấy |
|---|---|
| Nhập số bàn 05, không nhập mật khẩu | Vào được, chỉ thấy Bàn 05 |
| Gọi món đang "Tạm hết" | Bị từ chối, có thông báo |
| Bấm "Yêu cầu thanh toán" | Thu ngân nhận thông báo, **khách không tự đóng đơn** |

### Bài C. Nhân viên
| Làm gì | Phải thấy |
|---|---|
| `waiter` đăng nhập | Không có nút Thanh toán |
| `waiter` hủy món đang pha | Bị từ chối |
| `cashier1` thanh toán bàn 05 | Bàn về trạng thái trống, in hóa đơn |
| `cashier1` thử vào trang Admin | Không có lối vào |

### Bài D. Pha chế
| Làm gì | Phải thấy |
|---|---|
| Bấm "Tạm hết" món A | Khách và nhân viên thấy món A **xám + chữ "Tạm hết"** gần như ngay |
| Bấm hoàn thành món | Có ghi lại **ai** làm |

### Bài E. Admin – phần Quản lý quán
| Làm gì | Phải thấy |
|---|---|
| `manager` đăng nhập | Thấy tab quản lý quán; tab Nhân sự chỉ xem; **không** có đổi cổng/tắt server |
| Đổi giá món A | Mọi máy khách thấy giá mới |
| `admin` (Chủ) sửa nhân sự | Sửa được |

### Bài F. Admin – phần Quản lý máy chủ
| Làm gì | Phải thấy |
|---|---|
| `manager` vào phần máy chủ | Bị chặn |
| `admin` vào từ máy 2 | Phải nhập lại mật khẩu; chỉ xem phiên, ping, duyệt |
| `admin` thử đổi cổng/tắt server từ máy 2 | Không có nút |
| Đổi cổng tại máy Server | Làm được (sau khi nhập mật khẩu) |

### Bài G. Kick và xin vào lại
| Làm gì | Phải thấy |
|---|---|
| Chủ kick máy 2 | Máy 2 hiện hộp thông báo đẹp, tự về màn đăng nhập, đèn đỏ |
| Chờ 10 giây | Máy 2 **không tự vào lại** |
| Máy 2 bấm "Xin cấp lại" | Server hiện: máy nào, bàn nào, ai, giờ nào |
| Chủ bấm Duyệt | Máy 2 đèn xanh, đăng nhập lại được |
| Chủ bấm Từ chối | Máy 2 báo lý do, vẫn đèn đỏ |

### Bài H. Ping (kiểm tra mạng)
| Làm gì | Phải thấy |
|---|---|
| Bấm Ping 1 dòng | Hiện "Đang đo…" rồi hiện số ms đúng dòng đó |
| Bấm Ping toàn bộ | Từng dòng cập nhật số ms, bảng không bị giật |
| Rút mạng 1 máy rồi Ping | Dòng đó báo "Timeout" |

### Bài I. Mất mạng
| Tình huống | Phải thấy |
|---|---|
| Tắt Server lúc khách đang gọi món | Khách thấy báo mất kết nối; bật lại **không bị tạo đơn trùng** |
| 2 nhân viên cùng chuyển bàn 05 | Người sau bị từ chối và thấy trạng thái mới |

### Bài K. Ca trực
| Làm gì | Phải thấy |
|---|---|
| Thu ngân bán 3 đơn (2 tiền mặt, 1 chuyển khoản) | Tổng ca đúng |
| Mở "Hồ sơ & Ca trực" trên 2 máy khác nhau | Số liệu giống nhau |
| Kết ca nhập sai tiền | Hiện chênh lệch thừa/thiếu |

---

## BẢNG GHI KẾT QUẢ
| Bài | Nội dung | Trước khi sửa | Sau khi sửa |
|---|---|:-:|:-:|
| A | Cùng một dữ liệu | ☐ | ☐ |
| B | Khách | ☐ | ☐ |
| C | Nhân viên | ☐ | ☐ |
| D | Pha chế | ☐ | ☐ |
| E | Admin – quản lý quán | ☐ | ☐ |
| F | Admin – quản lý máy chủ | ☐ | ☐ |
| G | Kick / xin vào lại | ☐ | ☐ |
| H | Ping | ☐ | ☐ |
| I | Mất mạng | ☐ | ☐ |
| K | Ca trực | ☐ | ☐ |

---

## TÓM TẮT 3 DÒNG
1. **Nhân viên đang xem dữ liệu bằng "sổ riêng" thay vì hỏi Server.** Cần đổi thành: luôn hỏi Server qua mạng.
2. **Admin chia 2 phần: "Quản lý quán" và "Quản lý máy chủ".** Cần ghi rõ tab nào thuộc phần nào, và tách Chủ khác Quản lý.
3. **Làm Bài A trước** để biết chắc dữ liệu có đang lệch hay không.
