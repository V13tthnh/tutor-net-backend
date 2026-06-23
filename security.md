Dưới đây là danh sách chi tiết các vấn đề bảo mật bạn cần thực hiện cho website, chia theo Frontend và Backend:
1. Phía Frontend (Client-side)
Dù Client không an toàn, bạn vẫn cần thực hiện các biện pháp sau để bảo vệ trình duyệt và dữ liệu của người dùng:

- Chống XSS trên giao diện: Đối với DOM-based XSS, không sử dụng innerHTML mà hãy sử dụng textContent để hiển thị dữ liệu
. Nếu dùng các framework hiện đại (như React), hãy tận dụng tính năng auto-escape của chúng
. Bảo mật Cookie: Khi lưu trữ thông tin vào cookie, cần thiết lập cờ HttpOnly để chống XSS đọc trộm cookie và SameSite=Strict kèm thuộc tính secure để chống tấn công giả mạo request (CSRF)
. Gửi Token đúng chuẩn: Không được gửi JWT (JSON Web Token) qua URL, mà phải gửi đúng cách qua Header Authorization: Bearer <token>
. Mã hóa đường truyền: BẮT BUỘC sử dụng HTTPS thay cho HTTP để mã hóa dữ liệu, ngăn chặn tin tặc nghe lén (sniffing) và đánh cắp thông tin nhạy cảm

2. Phía Backend (Server-side & API)
Đây là tuyến phòng thủ quan trọng nhất. Bạn cần áp dụng các biện pháp sau:
A. Xác thực và Phân quyền (Authentication & Authorization)
Quản lý JWT an toàn: Khi sử dụng JWT, bắt buộc phải kiểm tra chữ ký (Verify Signature) và thời gian hết hạn (exp) trước khi xử lý request
. Tuyệt đối không cho phép sử dụng token có chữ ký alg=none
. Bảo vệ Secret Key: Cần sử dụng thuật toán mã hóa mạnh (HS256 với secret key dài, ngẫu nhiên hoặc RS256 với public/private key)
. Không lưu mật khẩu hoặc dữ liệu nhạy cảm dạng plain text trong payload của JWT vì ai cũng có thể giải mã được
. Cơ chế Token ngắn hạn: Access Token nên có thời gian sống ngắn (15-30 phút), kết hợp với Refresh Token có thời gian sống dài hơn
. Khi người dùng Logout, Refresh Token phải bị vô hiệu hóa và không được phép sử dụng lại
. Phân quyền chặt chẽ (RBAC): Đừng chỉ tin tưởng thông tin Role (quyền) nằm trong JWT Payload. Quyền truy cập bắt buộc phải được kiểm tra lại (query) từ Cơ sở dữ liệu của hệ thống
. Từ chối mọi request không có token hoặc dùng token sai (trả về HTTP 401)
. Kiểm soát luồng gọi API: Cần giới hạn số lần truy cập API (Rate limit) để chống brute-force và kiểm tra quyền đối với từng object (phòng lỗ hổng BOLA/IDOR)

B. Ngăn chặn tấn công Injection (Chèn mã độc)
Chống SQL Injection: Tuyệt đối không dùng phương pháp cộng/ghép chuỗi SQL với dữ liệu đầu vào. BẮT BUỘC sử dụng Prepared Statement để hệ thống hiểu dữ liệu chỉ là giá trị, không phải là câu lệnh
. Ngoài ra, không cấp quyền root cho user Database và không hiển thị lỗi SQL trực tiếp ra màn hình
. Chống Path Traversal (Truy cập thư mục trái phép): Không cho phép người dùng chỉ định đường dẫn file trực tiếp. Hãy sử dụng danh sách cho phép (Whitelist) hoặc hàm realpath() để đảm bảo file được truy cập nằm đúng trong thư mục an toàn
. Chống Command Injection: Nếu có tính năng chạy lệnh hệ thống, phải dùng hàm như escapeshellarg() để escape ký tự đặc biệt, kết hợp với việc chạy web server bằng quyền (privilege) thấp

C. Chống mã độc giao diện (HTML Injection & XSS)
Mọi dữ liệu từ người dùng trước khi in ra màn hình (echo) phải được làm sạch. Khuyến nghị sử dụng htmlspecialchars() để chuyển đổi các thẻ HTML thành văn bản thuần túy (text), giúp ngăn chặn mã JavaScript độc hại thực thi
. Nếu bắt buộc phải cho người dùng dùng thẻ HTML, hãy áp dụng strip_tags() kết hợp với danh sách các thẻ an toàn (Whitelist)
. Triển khai thêm Content Security Policy (CSP) ở phía server

D. Chống giả mạo yêu cầu (CSRF)
Mọi request có tính chất thay đổi dữ liệu (POST/PUT/DELETE) bắt buộc phải có CSRF Token (một chuỗi ngẫu nhiên sinh ra từ server và gắn vào form)
. Có thể kết hợp thêm việc kiểm tra nguồn gốc request qua Origin hoặc Referer Header

E. Cấu hình bảo mật và Giám sát (Security Misconfiguration & Monitoring)
Tránh các lỗi cấu hình cơ bản như để lộ các file .env, config.php hoặc bật tính năng Directory listing
. Thực hiện ghi nhật ký (Logging) và giám sát (Monitoring) đầy đủ để phát hiện các truy cập bất thường
. Cần kiểm tra kỹ các form upload file để chống việc tải lên mã độc (Webshell)
