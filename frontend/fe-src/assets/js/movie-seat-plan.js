// =============================================================
// GLOBAL VARIABLES
// =============================================================
var stompClient = null;
var currentUserId = getUserIdFromToken(); // Lấy User ID thật từ Token ngay khi vào trang

$(async function() {
    $('.preloader').fadeIn(100);

    // 1. Kiểm tra tham số URL
    let showId = getParam('show-id')
    if (!showId)
        return window.location.href = ('/movies')

    // 2. Lấy thông tin Suất chiếu (Show Info)
    let show = await PenguRequestAPI('GET',`api/show/${showId}`, {},  {},  true)
        .then(r => r.json())
        .catch(error => {console.log(error); return false})

    if (!show)
        return window.location.href = ('/')

    // Cập nhật giao diện thông tin phim
    $(".title.movie").text(show.movieName)
    $(".hall").text("Pengu Ha Noi - " + show.hallName)
    $(".date").text(GetTimeFormat(new Date(show.startTime)))
    $(".back-button").attr("href", "/booking?movie-id=" + encodeURI(show.movieId))

    // 3. Lấy danh sách Ghế (Seat List)
    let showSeats = await PenguRequestAPI('GET',`api/show/${showId}/seats`, {},  {},  true)
        .then(r => r.json())
        .catch(error => {console.log(error); return false})

    if (!showSeats)
        return window.location.href = ('/')

    // Gom nhóm ghế theo hàng (Row)
    let seatsByRow = groupBy(showSeats, d => d.rowIndex)

    $(".seat-area").empty()

    // 4. Vẽ sơ đồ ghế (Render Seats)
    for (let row in seatsByRow) {
        let rowSeats = seatsByRow[row]
        let div = $(`            
        <li class="seat-line">
            <span class='row-name'>f</span>
            <ul class="seat--area">
                <li class="front-seat">
                    <ul class="seats">
                      
                    </ul>
                </li>
            </ul>
            <span class='row-name'>f</span>
        </li>
        `)

        for (let i = 0; i < rowSeats.length; i++) {
            let seat = rowSeats[i]
            let colName = seat.colIndex + 1
            let rowName = seat.name.slice(0,-colName.toString().length).trim()

            div.find(".row-name").text(rowName)

            // [QUAN TRỌNG] Thêm ID vào thẻ li để Socket dễ tìm thấy ghế
            let item = $(`
            <li class="single-seat" id="seat-${seat.seatId}">
                <img src="assets/images/movie/seat01.png" alt="seat">
                <span class="sit-num">f7</span>
            </li>
            `)
            item.data('seat-id', seat.seatId)
            item.data('seat-price', seat.price)
            item.find(".sit-num").text(seat.name)

            // Xử lý trạng thái ban đầu của ghế
            if (seat.status === "AVAILABLE") {
                item.addClass("seat-free")
                item.find("img").attr("src", "assets/images/movie/seat01-free.png")
            }
            else if (seat.status === "PENDING") {
                // Ghế đang được giữ bởi người khác
                item.find("img").attr("src", "assets/images/movie/seat01-pending.png")
                item.find(".sit-num").remove()
            }
            else {
                // Ghế đã bán (BOOKED / UNAVAILABLE)
                item.find(".sit-num").remove()
            }

            div.find(".seats").append(item)
        }

        $(".seat-area").append(div)
    }

    // 5. Kết nối WebSocket (Real-time)
    connectWebSocket(showId);

    // 6. Xử lý đếm ngược (Countdown)
    let countdown = 5*60
    setInterval(async () => {
        if (countdown > 0) {
            countdown--;
            $(".countdown").text(`${Math.floor(countdown/60).toString().padStart(2, '0')}:${(countdown%60).toString().padStart(2, '0')}`)
        }
        else if (!swal.isVisible()) {
            await Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Bạn đã hết thời gian chọn ghế',
            })

            return window.location.href = ('/')
        }
    }, 1000)

    $('.preloader').fadeOut(1000);

    // 7. Xử lý sự kiện CLICK chọn ghế
    $('.seat-area').on("click", ".seat-free", function (e) {

        // [CHECK LOGIN] Yêu cầu đăng nhập mới được chọn ghế
        if (!currentUserId) {
            return Swal.fire({
                icon: 'warning',
                title: 'Yêu cầu đăng nhập',
                text: 'Vui lòng đăng nhập để thực hiện chọn ghế!',
                showCancelButton: true,
                confirmButtonText: 'Đăng nhập ngay',
                cancelButtonText: 'Hủy'
            }).then((result) => {
                if (result.isConfirmed) {
                    // Chuyển hướng sang trang login, kèm url hiện tại để quay lại
                    window.location.href = '/sign-in?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
                }
            });
        }

        let div = $(this)
        let isBooked = div.hasClass("seat-booked")

        // Update giao diện ngay lập tức (Optimistic UI update)
        if (isBooked)
            div.find("img").attr("src", "assets/images/movie/seat01-free.png")
        else
            div.find("img").attr("src", "assets/images/movie/seat01-booked.png")

        div.toggleClass("seat-booked", !isBooked)

        // Tính lại tổng tiền
        calculateTotal();

        // [WEBSOCKET] Gửi thông báo cho Server
        if (stompClient) {
            let action = !isBooked ? "SELECT" : "UNSELECT"; // Đảo ngược logic: Nếu vừa bấm -> là chọn
            let payload = {
                showId: showId,
                seatId: div.data('seat-id'),
                userId: currentUserId, // Gửi ID người dùng hiện tại
                action: action
            };

            // Gửi lên endpoint: /app/select-seat
            try {
                stompClient.send("/app/select-seat", {}, JSON.stringify(payload));
            } catch (err) {
                console.error("Không thể gửi socket:", err);
            }
        }
    });

    // 8. Xử lý sự kiện CLICK nút "Đặt vé"
    $("#create").click(async () => {
        // Kiểm tra đăng nhập lần nữa cho chắc
        if (!currentUserId) {
            return Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Bạn chưa đăng nhập!'});
        }

        let booked = $(".seat-free.seat-booked")

        if (booked.length == 0)
            return Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Vui lòng chọn ghế ngồi',
            })

        if (booked.length > 4)
            return Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Mỗi lần bạn chỉ có thể đặt tối đa 4 ghế',
            })

        let seatIds = []
        for (let i = 0; i < booked.length; i++) {
            let seat = booked.eq(i)
            seatIds.push(seat.data("seat-id"))
        }

        // Gọi API đặt vé
        let booking = await PenguRequestAPI('POST',`api/booking/add`, {
            "body" : {
                "show_id" : showId,
                "seat_ids" : seatIds
            }
        },  { "Content-Type": "application/json" },  true)
            .then(r => r.json())
            .catch(error => {console.log(error); return false})

        // Xử lý kết quả trả về
        if (!booking || !booking.status || booking.status == "NOT_FOUND" || booking.status == "FORBIDDEN")  {
            let msg = booking && booking.message ? booking.message : "Có lỗi xảy ra";
            await Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Có lỗi xảy ra: ' + msg,
            })
            return window.location.href =  $(".back-button").attr("href")
        }

        if (booking.status == "CONFLICT") {
            await Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Ghế bạn chọn hiện đã không còn trống',
            })
            return location.reload();
        }

        if (booking.status != "PENDING") {
            await Swal.fire({
                icon: 'error',
                title: 'Thông báo',
                text: 'Có lỗi xảy ra vui lòng thử lại sau',
            })
            return location.reload();
        }

        return window.location.href = ('/checkout?booking-id=' + encodeURIComponent(booking.id))
    })

    $('.preloader').fadeOut(1000);
})


// =============================================================
// HELPER FUNCTIONS
// =============================================================

function GetTimeFormat(time) {
    return `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')} - ${time.getDate().toString().padStart(2, '0')}/${(time.getMonth() + 1).toString().padStart(2, '0')}`
}

// Hàm tính tổng tiền (Tách ra để tái sử dụng khi socket update)
function calculateTotal() {
    let booked = $(".seat-free.seat-booked")
    let bookedSeat = []
    let totalPrice = 0

    for (let i = 0; i < booked.length; i++) {
        let seat = booked.eq(i)
        bookedSeat.push(seat.find(".sit-num").text())
        totalPrice += seat.data('seat-price')
    }

    $(".seat-book-list").text(bookedSeat.join(", "))
    $(".seat-price").text(totalPrice.toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 9}))
}

// =============================================================
// WEBSOCKET & TOKEN LOGIC
// =============================================================

// Hàm lấy UserID từ Token (Decode JWT)
function getUserIdFromToken() {
    // Lấy token từ Cookie (Tên cookie thường là 'token' hoặc 'accessToken')
    // Dựa vào file requestAPI.js hoặc sign-in.js của bạn để biết chính xác tên
    let token = $.cookie('pengu_token');

    if (!token) return null; // Chưa đăng nhập

    try {
        // Giải mã Payload của JWT
        let base64Url = token.split('.')[1];
        let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        let jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        let payload = JSON.parse(jsonPayload);

        // Trả về User ID (thường nằm ở field 'sub' hoặc 'userId')
        // Dựa trên chuỗi JWT mẫu bạn gửi: "sub": "user_2"
        return payload.sub || payload.userId || payload.id;
    } catch (e) {
        console.error("Lỗi giải mã Token:", e);
        return null;
    }
}

// Hàm kết nối WebSocket
function connectWebSocket(showId) {
    // Endpoint phải trùng với config trong WebSocketConfig.java
    var backendUrl = GetIP();

    // Nếu đang chạy local mà GetIP() trả về sai, bạn có thể set cứng tạm thời:
    // var backendUrl = 'http://localhost:9595';

    var socket = new SockJS(backendUrl + '/ws-cinema');
    stompClient = Stomp.over(socket);

    // Tắt debug log để console gọn gàng (bật lại nếu cần sửa lỗi)
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('Connected to WebSocket');

        // Đăng ký lắng nghe Topic của Suất chiếu này
        stompClient.subscribe('/topic/show/' + showId, function (messageOutput) {
            var data = JSON.parse(messageOutput.body);
            handleSocketMessage(data);
        });
    }, function(error){
        console.log("WebSocket connect error: " + error);
    });
}

// Hàm xử lý tin nhắn nhận được từ Server
function handleSocketMessage(data) {
    // 1. Nếu tin nhắn do chính mình gửi -> Bỏ qua
    if (data.userId == currentUserId) return;

    // 2. Tìm thẻ ghế tương ứng
    let seatElement = $("#seat-" + data.seatId);
    if (seatElement.length === 0) return;

    console.log("Socket Update:", data);

    // 3. Cập nhật giao diện dựa trên hành động của người khác
    if (data.action === "SELECT") {
        // Người khác chọn -> Chuyển sang màu PENDING (xám/cam)
        seatElement.removeClass("seat-free"); // Xóa class này để không click được nữa
        seatElement.find("img").attr("src", "assets/images/movie/seat01-pending.png");

        // Cực kỳ quan trọng: Nếu MÌNH cũng đang chọn ghế này (tranh chấp) -> Phải bỏ chọn của mình đi
        if (seatElement.hasClass("seat-booked")) {
            seatElement.removeClass("seat-booked");
            calculateTotal(); // Trừ tiền đi

            // Thông báo nhẹ (Toast)
            const Toast = Swal.mixin({
                toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
            })
            Toast.fire({ icon: 'info', title: 'Ghế bạn chọn đã bị người khác đặt!' })
        }

    } else if (data.action === "UNSELECT") {
        // Người khác bỏ chọn -> Trả lại màu Available (trắng/xanh)
        seatElement.addClass("seat-free");
        seatElement.find("img").attr("src", "assets/images/movie/seat01-free.png");
    } else if (data.action === "SOLD") {
        // Vé đã bán thành công
        seatElement.removeClass("seat-free seat-booked");
        seatElement.find("img").attr("src", "assets/images/movie/seat01.png");
        seatElement.find(".sit-num").remove();
    }
}