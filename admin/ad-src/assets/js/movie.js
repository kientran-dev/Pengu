var movieList = []

$(async () => {
    $('.preloader').fadeIn(100);
    
    await GetList()
    $('.preloader').fadeOut(1000);
})

$(".ticket-search-form").on('submit', (e) => e.preventDefault());

$('#search-val').on('input', function() {
    RenderMovieList();
});


$("#movie-new").click(async () => {

    const { value: formValues } = await Swal.fire({
        title: 'Add New Movie',
        html:
            `
        <div class="form-group">
            <label for="movienew-title">Title</label>
            <input type="text" class="form-control" id="movienew-title">
        </div>
        <div class="form-group">
            <label for="movienew-image">Image URL</label>
            <input type="text" class="form-control" id="movienew-image">
        </div>
        <div class="form-group">
            <label for="movienew-large-image">Large Image URL</label>
            <input type="text" class="form-control" id="movienew-large-image">
        </div>
        <div class="form-group">
            <label for="movienew-trailer">Trailer URL</label>
            <input type="text" class="form-control" id="movienew-trailer">
        </div>
        <div class="form-group">
            <label for="movienew-durationInMins">Duration (Mins)</label>
            <input type="number" class="form-control" id="movienew-durationInMins">
        </div>
        <div class="form-group">
            <label for="movienew-country">Country</label>
            <input type="text" class="form-control" id="movienew-country">
        </div>
        <div class="form-group">
            <label for="movienew-language">Language</label>
            <input type="text" class="form-control" id="movienew-language">
        </div>
        <div class="form-group">
            <label for="movienew-actors">Actors</label>
            <input type="text" class="form-control" id="movienew-actors">
        </div>
        <div class="form-group">
            <label for="movienew-description">Description</label>
            <textarea class="form-control" id="movienew-description" rows="3"></textarea>
        </div>
        <div class="form-group">
            <label for="movienew-releaseDate">Release Date</label>
            <input type="date" class="form-control" id="movienew-releaseDate">
        </div>
        `
        ,
        focusConfirm: false,
        showCloseButton: true,
        confirmButtonText: 'Add Movie',
        cancelButtonText: 'Cancel',
        showCancelButton: true,
        preConfirm: () => {
            return {
                // No ID sent here
                title : $('#movienew-title').val(),
                image : $('#movienew-image').val(),
                large_image: $('#movienew-large-image').val(),
                trailer : $('#movienew-trailer').val(),
                durationInMins : $('#movienew-durationInMins').val(),
                country : $('#movienew-country').val(), // FIXED TYPO
                language : $('#movienew-language').val(),
                actors : $('#movienew-actors').val(),
                description : $('#movienew-description').val(),
                releaseDate : $('#movienew-releaseDate').val(),
            }
        }
    })

    if (!formValues) return

    // Basic Validation
    if(!formValues.title || !formValues.durationInMins) {
        Swal.fire('Error', 'Title and Duration are required!', 'error');
        return;
    }

    let response = await PenguRequestAPI( 'POST','api/movie/add',
        {
            "body" : formValues
        },
        { "Content-Type": "application/json"}, true) .then(r => r.json()).catch(error => {console.log(error); return false})

    if (!response) return

    Swal.fire({
        position: 'top-end',
        icon: 'success',
        title: 'Success',
        text: response.message || 'Movie added successfully!',
        showConfirmButton: false,
        timer: 2000
    })

    await GetList()
})


$("#modal-movie-delete").click(async () => {
    let username = $("#movie-modal-container #movie-name").text()

    let result = await Swal.fire({
        title: 'Chắc chắn chứ?',
        text: "Hành động này sẽ không thể hoàn tác",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Vâng, tôi chắc chắn',
        cancelButtonText: 'Hủy bỏ',
    })

    if (!result.isConfirmed) 
        return;

    let response = await PenguRequestAPI( 'DELETE','api/movie/delete?username=' + encodeURI(username), {},  {},  true) .then(r => r.json()).catch(error => {console.log(error); return false})
    if (!response)
        return

    Swal.fire({
        position: 'top-end',
        text: response.message,
        backdrop : false,
        showConfirmButton: false,
        timer: 3000
      })

    await GetList()
})

$("#body-container").on("click", "tr #movie-edit", async (e) => {
    let div = $(e.currentTarget).closest('tr')
    let id = div.find("#movie-id").text()

    let movie = await PenguRequestAPI( 'GET','api/movie/' + encodeURIComponent(id), {},  {},  true) .then(r => r.json()).catch(error => {console.log(error); return false})
    if (!movie) return;


    const { value: formValues } = await Swal.fire({
        title: 'Edit Movie',
        html:
            `
        <div class="form-group">
            <label for="movienew-id">ID</label>
            <input type="text" class="form-control" id="movienew-id" value="${formatContentHTML(movie.id)}" readonly style="background-color: #e9ecef;">
        </div>
        <div class="form-group">
            <label for="movienew-title">Title</label>
            <input type="text" class="form-control" id="movienew-title" value="${formatContentHTML(movie.title)}">
        </div>
        <div class="form-group">
            <label for="movienew-image">Image URL</label>
            <input type="text" class="form-control" id="movienew-image" value="${formatContentHTML(movie.image)}">
        </div>
         <div class="form-group">
            <label for="movienew-large-image">Large Image URL</label>
            <input type="text" class="form-control" id="movienew-large-image" value="${formatContentHTML(movie.large_image || '')}">
        </div>
        <div class="form-group">
            <label for="movienew-trailer">Trailer URL</label>
            <input type="text" class="form-control" id="movienew-trailer" value="${formatContentHTML(movie.trailer)}">
        </div>
        <div class="form-group">
            <label for="movienew-durationInMins">Duration (Mins)</label>
            <input type="number" class="form-control" id="movienew-durationInMins" value="${formatContentHTML(movie.durationInMins)}">
        </div>
        <div class="form-group">
            <label for="movienew-country">Country</label>
            <input type="text" class="form-control" id="movienew-country" value="${formatContentHTML(movie.country)}">
        </div>
        <div class="form-group">
            <label for="movienew-language">Language</label>
            <input type="text" class="form-control" id="movienew-language" value="${formatContentHTML(movie.language)}">
        </div>
        <div class="form-group">
            <label for="movienew-actors">Actors</label>
            <input type="text" class="form-control" id="movienew-actors" value="${formatContentHTML(movie.actors)}">
        </div>
        <div class="form-group">
            <label for="movienew-description">Description</label>
            <textarea class="form-control" id="movienew-description" rows="3">${formatContentHTML(movie.description)}</textarea>
        </div>
        <div class="form-group">
            <label for="movienew-releaseDate">Release Date</label>
            <input type="text" class="form-control" id="movienew-releaseDate" value="${formatContentHTML(movie.releaseDate)}">
        </div>
        `
        ,
        focusConfirm: false,
        showCloseButton: true,
        confirmButtonText: 'Save Changes',
        cancelButtonText: 'Cancel',
        showCancelButton: true,
        preConfirm: () => {
            return {
                id : $('#movienew-id').val(),
                title : $('#movienew-title').val(),
                image : $('#movienew-image').val(),
                large_image : $('#movienew-large-image').val(),
                trailer : $('#movienew-trailer').val(),
                durationInMins : $('#movienew-durationInMins').val(),
                country : $('#movienew-country').val(), // FIXED TYPO
                language : $('#movienew-language').val(),
                actors : $('#movienew-actors').val(),
                description : $('#movienew-description').val(),
                releaseDate : $('#movienew-releaseDate').val(),
            }
        }
    })


    if (!formValues) return


    let response = await PenguRequestAPI( 'PUT',`api/movie/${encodeURIComponent(id)}/edit`,
        {
            "body" : formValues
        },
        { "Content-Type": "application/json"}, true) .then(r => r.json()).catch(error => {console.log(error); return false})

    if (!response) return

    Swal.fire({
        position: 'top-end',
        icon: 'success',
        title: 'Updated',
        text: response.message || 'Movie updated successfully!',
        showConfirmButton: false,
        timer: 2000
    })

    await GetList()
})

async function GetList() {  
    let list = await PenguRequestAPI( 'GET','api/movie/getall', {},  {},  true) .then(r => r.json()).catch(error => {console.log(error); return false})
    if (list)
        movieList = list;

    RenderMovieList()
}

// Hàm chuyển đổi phút sang Giờ và Phút
function convertMinutesToTime(minutes) {
    if (!minutes || minutes < 0) return "0m";

    const h = Math.floor(minutes / 60);
    const m = minutes % 60;

    // Nếu tròn giờ (ví dụ 60p -> 1h)
    if (m === 0) return `${h}h`;
    // Nếu chưa đủ 1 giờ (ví dụ 45p -> 45m)
    if (h === 0) return `${m}m`;

    // Bình thường (ví dụ 130p -> 2h 10m)
    return `${h}h ${m}m`;
}


function RenderMovieList() {
    $('#movie-modal-container').modal('hide')
    const container = $("#body-container")
    container.empty()

    let data = movieList.filter((m) => m.title.toLowerCase().indexOf($('#search-val').val().toLowerCase()) != -1)

    for (let i = 0; i < data.length; i++) {
        let movie = data[i]

        if (movie) {
            let item = $(`
            <tr>
                <th scope="row" id="movie-id"></th>
                <td id="movie-name" style="font-weight: 600;"></td> <td id="movie-duration"></td>
                <td id="movie-release"></td>
                <td> <button type="button" class="btn btn-info" id="movie-edit"><i class="fas fa-edit"></i>Edit</button> </td>
            </tr>
            `)

            item.find("#movie-id").text(movie.id)
            item.find("#movie-name").text(movie.title)

            // --- PHẦN SỬA ĐỔI ĐỂ HIỂN THỊ ĐẸP HƠN ---
            // Sử dụng badge màu cam nhẹ hoặc xanh, thêm icon đồng hồ
            let prettyDuration = `
                <span class="badge badge-pill badge-warning text-white" style="font-size: 0.9em; padding: 8px 12px;">
                    <i class="far fa-clock mr-1"></i> ${convertMinutesToTime(movie.durationInMins)}
                </span>
            `;
            item.find("#movie-duration").html(prettyDuration)
            // ----------------------------------------

            item.find("#movie-release").text(movie.releaseDate)

            container.append(item)
        }
    }
}

