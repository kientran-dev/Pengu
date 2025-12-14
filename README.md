# 🎬 Pengu Cinema - Website đặt vé cho rạp phim Pengu

[![Monorepo](https://img.shields.io/badge/Monorepo-Admin%20%7C%20Backend%20%7C%20Frontend-1f6feb)](#-cau-truc-thu-muc)
[![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](#-chay-nhanh-voi-docker-compose)
[![Postman](https://img.shields.io/badge/API-Postman%20Collection-FF6C37?logo=postman&logoColor=white)](#-tai-lieu-api-postman)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Status](https://img.shields.io/badge/Status-Active-success)](#-trang-thai-du-an)

Ứng dụng được tổ chức theo dạng monorepo, bao gồm:
- 🧩 Admin: Giao diện quản trị
- 🚀 Backend: Dịch vụ API
- 🎨 Frontend: Giao diện người dùng
- 🐳 Docker Compose: Dàn dựng môi trường phát triển/chạy thử
- 📫 Bộ sưu tập Postman để thử API
- 🖼️ Ảnh/tài nguyên minh họa

---

## 🌐 Demo

- **User App:** [https://pengu-frontend.onrender.com](https://pengu-frontend.onrender.com)
- **Admin App:** [https://pengu-backend.onrender.com](https://pengu-backend.onrender.com)

---

## 🗂️ Cấu trúc thư mục

```text
.
├─ .gitattributes
├─ .idea/                  # ⚙️ Cấu hình IDE (IntelliJ/JetBrains)
├─ .vscode/                # ⚙️ Cấu hình VS Code
├─ admin/                  # 🧩 Ứng dụng quản trị (Admin)
├─ backend/                # 🚀 Dịch vụ API (Backend)
├─ frontend/               # 🎨 Ứng dụng người dùng (Frontend)
├─ docker-config/          # 🐳 Cấu hình phục vụ docker compose (nếu có)
├─ docker-compose.yaml     # 🐳 Orchestration với Docker Compose
├─ cinema_controllers.postman.json  # 📫 Bộ sưu tập Postman cho API
├─ images/                 # 🖼️ Tài nguyên hình ảnh
└─ bruteforce.py           # 🧪 Script tiện ích/thử nghiệm (Python)
```
---

## ⚡ Tính năng chính

- 🧱 Monorepo: gom admin, backend, frontend trong một kho mã
- 🐳 Docker Compose: chạy nhanh tất cả dịch vụ cục bộ
- 📫 Postman Collection: dễ dàng khám phá và thử API
- 🖼️ Thư mục `images/`: lưu asset minh họa/tài liệu

---

## 🛠️ Công nghệ (tổng quan)

- 🐳 Docker, Docker Compose
- 🌐 RESTful API (chi tiết trong `backend/`)
- 💼 Admin/Frontend SPA (chi tiết trong `admin/`, `frontend/`)
- 🧪 Postman cho thử nghiệm API

Nếu backend sử dụng Java/Spring (thường gặp trong dự án dạng này), gợi ý stack:
- ☕ Java 21, Spring Boot 3.x, Spring Data (JPA), Spring Security (JWT), MySQL
- 📦 Lombok, MapStruct (tuỳ chọn), Validation API

---

## 🚀 Chạy nhanh với Docker Compose

Yêu cầu:
- Docker & Docker Compose
- (Tùy chọn) Biến môi trường/tệp cấu hình trong `docker-config/` nếu được tham chiếu trong `docker-compose.yaml`

Các bước:
1) Kiểm tra và bổ sung cấu hình (nếu cần)
    - Mở `docker-compose.yaml` để xem các service, port, network, volume sử dụng.
    - Nếu `docker-config/` được tham chiếu (ENV file, cert, secret…), đặt đúng các tệp tại đây.

2) Dựng và chạy dịch vụ
   ```bash
   docker compose up -d --build
   ```

3) Theo dõi log
   ```bash
   docker compose logs -f
   ```

4) Dừng và dọn dẹp tài nguyên
   ```bash
   docker compose down -v
   ```

Sau khi chạy:
- Dùng `docker ps` để xem container và cổng (port) mở.
- Truy cập các endpoint/backend/frontend theo cổng ánh xạ thực tế in ra trong log.

---

## 📫 Tài liệu API (Postman)

- Tệp: [`cinema_controllers.postman.json`](./cinema_controllers.postman.json)
- Cách dùng:
    1) Mở Postman → File → Import
    2) Chọn tệp `cinema_controllers.postman.json`
    3) Tạo Environment chứa `{{base_url}}`, token… phù hợp với cổng/host khi chạy
    4) Gửi request và kiểm tra kết quả

Mẹo:
- Nếu backend yêu cầu xác thực, hãy thêm biến `{{access_token}}`/`{{refresh_token}}` trong Environment.

---

## 🧪 Tài khoản test

| Username           | Password     | Role        |
|--------------------|--------------|-------------|
| super_admin.1234   | 3Mt^tmM85YUL | Super Admin |
| admin.1234         | TiNkErBeLl   | Admin       |
| user_1             | k9G*Ni91r!   | User        |
| user_2             | hS5f%1*8V1   | User        |

## 💳 Thông tin thanh toán test (VNPAY)

| Ngân hàng | Số thẻ              | Tên chủ thẻ  | Ngày phát hành | Mật khẩu OTP |
|-----------|---------------------|--------------|----------------|--------------|
| NCB       | 9704198526191432198 | NGUYEN VAN A | 07/15          | 123456       |

---

## 💻 Phát triển cục bộ (không dùng Docker)

Do monorepo có nhiều thành phần, bạn có thể chạy từng phần riêng:

- Backend (API) — vào thư mục `backend/`:
    - Xem tệp build (`pom.xml`/`build.gradle`) và cấu hình (`application.yml`/`application.properties`) để biết cách chạy.
    - Chạy server theo công nghệ backend đang sử dụng (ví dụ Spring Boot: `./mvnw spring-boot:run` hoặc `./gradlew bootRun`).

- Frontend — vào thư mục `frontend/`:
    - Cài dependencies (ví dụ: `npm install` / `yarn`).
    - Chạy dev server (ví dụ: `npm run dev`).

- Admin — vào thư mục `admin/`:
    - Tương tự như frontend, tùy framework sử dụng.

Lưu ý:
- Cần đồng bộ biến môi trường giữa các thành phần, ví dụ URL backend cho frontend/admin.

---

## 🔧 Cấu hình & Môi trường

- `docker-config/`: Thư mục dành cho cấu hình phục vụ `docker-compose.yaml` (nếu được tham chiếu).
- `.env` (tùy chọn): Có thể sử dụng để cấu hình thông số (port, chuỗi kết nối DB, secret, v.v.) — đặt tại root hoặc trong từng dịch vụ (backend/frontend/admin) theo nhu cầu.
- Khuyến nghị thêm tệp mẫu `*.env.example` để tiện onboarding.


---

## 🧪 Kiểm thử

- Import Postman collection để thử nhanh API.
- Nếu có test tự động, xem hướng dẫn trong từng thư mục (backend/frontend/admin).
- Có `bruteforce.py` (Python) như một script thử nghiệm/tiện ích — cẩn trọng khi sử dụng.

---

## 🤝 Đóng góp

Đóng góp rất hoan nghênh!
- Tạo nhánh từ `main`.
- Commit rõ ràng, có mô tả.
- Tạo Pull Request kèm:
    - Ảnh minh họa/tài liệu (nếu có)
    - Cách kiểm thử
    - Ảnh hưởng backward compatibility (nếu thay đổi API)
    - Cập nhật README/ENV khi cần

Checklist gợi ý khi mở PR:
- [ ] Cập nhật tài liệu (README/ENV)
- [ ] Thêm/điều chỉnh test (nếu có)
- [ ] Đảm bảo chạy được bằng Docker Compose
- [ ] Không phá vỡ hợp đồng API hiện có (nếu có client đang dùng)

---

## 📄 Giấy phép

Phần mềm được phân phối theo giấy phép MIT. Xem tệp [LICENSE](./LICENSE).

---

## 🧭 Lộ trình gợi ý

- 🧪 Bổ sung test tự động cho backend/frontend
- 🛡️ Thêm CI (GitHub Actions) để lint/test/build
- 📘 Tài liệu chi tiết cho từng module (admin/backend/frontend)
- 📦 Chuẩn hóa biến môi trường (mẫu `.env.example`)
- 🧰 Thêm script tiện ích cho nhà phát triển (Makefile/NPM scripts)

---

## 📌 Trạng thái dự án

- Đang hoạt động. Vui lòng xem commit gần nhất và `docker-compose.yaml` để nắm phiên bản dịch vụ hiện tại.

---

Made with ❤️ by Tran Trung Kien