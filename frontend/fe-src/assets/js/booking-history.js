$(async () => {
    // 1. Kiểm tra Token
    if (!await TokenIsVaild()) {
        window.location.href = "sign-in.html";
        return;
    }

    $('.preloader').fadeIn(100);

    try {
        let response = await PenguRequestAPI('GET', 'api/booking/getall', {}, {}, true);

        if (response.status === 401 || response.status === 403) {
            RemoveToken();
            localStorage.removeItem('LAST_CHECK_TOKEN');
            window.location.href = "sign-in.html";
            return;
        }

        if (!response.ok) throw new Error("Lỗi kết nối server");

        let list = await response.json();

        // 2. Render dữ liệu
        if (list && Array.isArray(list)) {
            // Lọc vé thành công & Sắp xếp mới nhất lên đầu
            let bookedList = list.filter(item => item.status === 'BOOKED');
            bookedList.sort((a, b) => new Date(b.createAt) - new Date(a.createAt));

            // Hiển thị tên khách
            if (list.length > 0 && list[0].fullname) {
                $("#customer-info").html(`Xin chào, <span style="color: #31d7a9;">${list[0].fullname}</span>`);
            }

            const container = $("#history-container");
            container.empty();

            if (bookedList.length === 0) {
                $('#empty-msg').show();
            } else {
                $('#empty-msg').hide();

                bookedList.forEach(booking => {
                    // --- TRANG TRÍ ---
                    // Ghế màu xanh ngọc (hợp tông web)
                    let seatColor = "#31d7a9";

                    let item = $(`
                        <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                            <th scope="row">
                                <span style="color: #ccc; font-weight: normal;">#${booking.id.substring(0, 8)}</span>
                            </th>
                            
                            <td>
                                <span style="font-weight: bold; color: #fff; font-size: 15px; text-transform: uppercase;">
                                    ${booking.movieName}
                                </span>
                            </td>
                            
                            <td style="color: #e0e0e0; font-size: 14px;">${booking.hallName}</td>
                            
                            <td style="color: ${seatColor}; font-weight: bold; font-size: 15px;">
                                ${booking.seats.join(", ")}
                            </td>
                            
                            <td>
                                <div style="color: #fff;">${formatDateTime(booking.startTime, false)}</div>
                            </td>

                            <td>
                                <div style="color: #aaa; font-size: 13px;">${formatDateTime(booking.createAt, true)}</div>
                            </td>
                            
                            <td>
                                <span style="color: #f39c12; font-weight: bold; font-size: 15px;">
                                    ${booking.price.toLocaleString()} đ
                                </span>
                            </td>
                        </tr>
                    `);
                    container.append(item);
                });
            }
        } else {
            $('#empty-msg').show();
        }

    } catch (e) {
        console.error(e);
        $('#empty-msg').text("Có lỗi xảy ra khi tải lịch sử.").show();
    } finally {
        $('.preloader').fadeOut(1000);
    }
});

// Hàm format ngày giờ (isDateOnly: chỉ lấy ngày, false: lấy giờ + ngày)
function formatDateTime(dateString, isDateOnly) {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;

    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();

    if (isDateOnly) {
        return `${day}/${month}/${year}`; // Format cho cột Ngày đặt
    }
    return `${hours}:${minutes} <br> <span style="font-size:12px; color:#888">${day}/${month}/${year}</span>`; // Format cho cột Suất chiếu
}