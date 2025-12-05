$(document).ready(function() {

    // --- CẤU HÌNH DARK MODE CHO BIỂU ĐỒ ---
    Chart.defaults.font.family = 'Nunito, -apple-system,system-ui,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif';

    // Đổi màu chữ toàn bộ biểu đồ sang trắng xám để nổi trên nền tối
    Chart.defaults.color = '#d1d3e2';
    // Đổi màu đường kẻ lưới (grid) sang màu tối mờ
    Chart.defaults.borderColor = 'rgba(255, 255, 255, 0.1)';


    // Hàm định dạng tiền VND
    const formatCurrency = (value) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    };

    // Hàm viết gọn số lớn
    const numberFormatter = new Intl.NumberFormat('vi-VN', {
        notation: "compact",
        compactDisplay: "short"
    });


    // --- 1. Fetch Summary Statistics ---
    sendRequest('api/statistics/summary', 'GET', null, (data) => {
        $('#total-revenue').text(formatCurrency(data.totalRevenue));
        $('#tickets-sold').text(numberFormatter.format(data.ticketsSold));
        $('#total-customers').text(numberFormatter.format(data.customers));
        $('#total-movies').text(numberFormatter.format(data.movies));
    });


    // --- 2. Fetch Revenue Trend ---
    sendRequest('api/statistics/revenue-trend', 'GET', null, (data) => {
        const labels = data.map(item => item.date);
        const revenues = data.map(item => item.revenue);

        const ctx = document.getElementById('revenue-trend-chart').getContext('2d');

        let gradient = ctx.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(78, 115, 223, 0.5)'); // Tăng độ đậm màu xanh
        gradient.addColorStop(1, 'rgba(78, 115, 223, 0.0)');

        new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Doanh thu',
                    data: revenues,
                    fill: true,
                    backgroundColor: gradient,
                    borderColor: '#4e73df', // Màu đường line sáng
                    borderWidth: 3,
                    pointBackgroundColor: '#4e73df',
                    pointBorderColor: '#fff',
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    tension: 0.4
                }]
            },
            options: {
                maintainAspectRatio: false,
                layout: { padding: { left: 10, right: 25, top: 25, bottom: 0 } },
                scales: {
                    x: {
                        grid: { display: false, drawBorder: false },
                        ticks: { color: '#d1d3e2' } // Màu chữ trục X
                    },
                    y: {
                        ticks: {
                            color: '#d1d3e2', // Màu chữ trục Y
                            maxTicksLimit: 5,
                            padding: 10,
                            callback: function(value) { return numberFormatter.format(value); }
                        },
                        grid: {
                            color: "rgba(255, 255, 255, 0.05)", // Đường kẻ ngang rất mờ
                            drawBorder: false,
                            borderDash: [2],
                            zeroLineColor: "rgba(255, 255, 255, 0.05)"
                        }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: "rgba(0, 0, 0, 0.8)", // Tooltip nền đen
                        bodyColor: "#fff",
                        titleColor: '#fff',
                        borderColor: 'rgba(255, 255, 255, 0.2)',
                        borderWidth: 1,
                        displayColors: false,
                        callbacks: {
                            label: function(tooltipItem) {
                                return 'Doanh thu: ' + formatCurrency(tooltipItem.raw);
                            }
                        }
                    }
                }
            }
        });
    });


    // --- 3. Fetch Top Movies ---
    sendRequest('api/statistics/top-movies', 'GET', null, (data) => {
        const labels = data.map(item => item.title || item.name);
        const ticketsSold = data.map(item => item.tickets_sold);

        // Màu sắc tươi sáng hơn để nổi trên nền tối
        const backgroundColors = ['#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b'];

        const ctx = document.getElementById('top-movies-chart').getContext('2d');
        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: ticketsSold,
                    backgroundColor: backgroundColors,
                    hoverBorderColor: "#2c3e50", // Viền khi hover trùng màu nền card
                    borderColor: "#151f30",      // Viền các lát cắt trùng màu nền card
                    borderWidth: 4               // Viền dày để tạo khoảng cách rõ
                }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: {
                            color: '#d1d3e2', // Màu chữ chú thích trắng
                            padding: 20,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        backgroundColor: "rgba(0, 0, 0, 0.8)",
                        bodyColor: "#fff",
                        callbacks: {
                            label: function(context) {
                                let label = context.label || '';
                                if (label) label += ': ';
                                label += numberFormatter.format(context.raw) + ' vé';
                                return label;
                            }
                        }
                    }
                },
                cutout: '70%',
            }
        });
    });

});