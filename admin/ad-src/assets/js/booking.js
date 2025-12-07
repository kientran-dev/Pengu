var bookingList = []

$(async () => {
    $('.preloader').fadeIn(100);
    await GetList()
    $('.preloader').fadeOut(1000);
})

$(".ticket-search-form").on('submit', (e) => e.preventDefault());

$('#search-val').on('input', function() {
    RenderBookingList();
});

$("#body-container").on("click", "tr #booking-edit", async (e) => {
    let div = $(e.currentTarget).closest('tr')
    let id = div.find("#booking-id").text()
    let username = div.data("username");

    let booking = await PenguRequestAPI( 'GET',`api/booking/user/${encodeURIComponent(username)}/${encodeURIComponent(id)}`, {},  {},  true) .then(r => r.json()).catch(error => {console.log(error); return false})
    if (!booking) return;

    const { value: status } = await Swal.fire({
        title: 'Thông tin trạng thái',
        input: 'radio',
        inputOptions: {
            'PENDING': 'Đang xử lý',
            'BOOKED': 'Đặt thành công',
            'CANCLED': 'Hủy'
        },
        focusConfirm: false,
        showCloseButton: true
    })

    if (!status) return

    let response = await PenguRequestAPI( 'PUT',`api/booking/user/${encodeURIComponent(username)}/${encodeURIComponent(id)}/setstatus?value=${encodeURIComponent(status)}`, {}, {}, true) .then(r => r.json()).catch(error => {console.log(error); return false})

    if (!response) return

    Swal.fire({
        position: 'top-end',
        text: response.message,
        backdrop : false,
        showConfirmButton: false,
        timer: 3000
    })

    await GetList()
})

async function GetList() {
    let list = await PenguRequestAPI('GET', `api/booking/admin/getall-system`, {}, {}, true)
        .then(r => r.json())
        .catch(error => { console.log(error); return false });

    if (list)
        bookingList = list;

    RenderBookingList()
}

function RenderBookingList() {
    $('#booking-modal-container').modal('hide')
    const container = $("#body-container")
    container.empty()

    let keyword = $('#search-val').val().toLowerCase().trim();

    let data = bookingList.filter((m) => {
        let idMatch = m.id && m.id.toString().toLowerCase().indexOf(keyword) != -1;
        let userMatch = m.username && m.username.toLowerCase().indexOf(keyword) != -1;
        let nameMatch = m.fullname && m.fullname.toLowerCase().indexOf(keyword) != -1;
        return idMatch || userMatch || nameMatch;
    })

    for (let i = 0; i < data.length; i++) {
        let booking = data[i]

        if (booking) {
            let item = $(`
            <tr>
                <th scope="row" id="booking-id"></th>
                <td id="booking-user" style="font-weight:bold; color: #ebf6fa;"></td>
                <td id="booking-movie"></td>
                <td id="booking-hall"></td>
                <td id="booking-show"></td>
                <td id="booking-seat"></td>
                <td id="booking-price"></td>
                <td id="booking-status"></td>
                <td id="booking-time" style="color: #ffff00;"></td> <td> <button type="button" class="btn btn-info" id="booking-edit">Status</button> </td>
            </tr>
            `)

            item.data("username", booking.username);

            item.find("#booking-id").text(booking.id)

            let userDisplay = booking.username;
            if (booking.fullname) userDisplay += ` (${booking.fullname})`;
            item.find("#booking-user").text(userDisplay);

            // Hiển thị tên phim kèm Giờ chiếu để Admin tiện tra cứu
            let movieInfo = booking.movieName;
            if (booking.startTime) {
                movieInfo += ` [${GetTimeFormat(new Date(booking.startTime), false)}]`;
            }
            item.find("#booking-movie").text(movieInfo)

            item.find("#booking-hall").text(booking.hallName)
            item.find("#booking-show").text(booking.showId)
            item.find("#booking-seat").text(booking.seats.join(", "))
            item.find("#booking-price").text(booking.price.toLocaleString() + ' đ') // Format tiền tệ
            item.find("#booking-status").text(booking.status)

            // SỬA: Hiển thị Ngày Đặt (createAt) thay vì StartTime
            // booking.createAt khớp với phương thức getCreateAt() trong Java
            if (booking.createAt) {
                item.find("#booking-time").text(GetTimeFormat(new Date(booking.createAt), true))
            } else {
                item.find("#booking-time").text("N/A");
            }

            container.append(item)
        }
    }
}

// Hàm format time: isFullDate = true sẽ hiện cả ngày tháng năm đầy đủ
function GetTimeFormat(time, isFullDate = true) {
    if (isNaN(time.getTime())) return ""; // Check invalid date

    let hours = time.getHours().toString().padStart(2, '0');
    let minutes = time.getMinutes().toString().padStart(2, '0');
    let day = time.getDate().toString().padStart(2, '0');
    let month = (time.getMonth() + 1).toString().padStart(2, '0');
    let year = time.getFullYear();

    if (isFullDate) {
        return `${hours}:${minutes} - ${day}/${month}/${year}`;
    } else {
        return `${hours}:${minutes}`; // Chỉ hiện giờ (dùng cho cột Movie)
    }
}