/* Módulo de reportes: pide los datos al servidor (JSON) y los dibuja con Chart.js.
 * El tipo de gráfico se elige en el navegador, sin volver a consultar la base de datos.
 * Para agregar un reporte nuevo: un endpoint /admin/reportes/api/xxx, un bloque .report-card
 * en reportes.html y una entrada en REPORTS. */
(function () {
    'use strict';

    var COLORS = ['#0f4c81', '#e67e22', '#2e9e6b', '#c0392b', '#8e44ad',
        '#16a5b8', '#d4ac0d', '#7f8c8d', '#e84393', '#5d6d7e'];

    // allowed: tipos que ofrece cada reporte; def: el que se muestra al abrir.
    var TYPES = {
        bar:       {label: 'Barras verticales'},
        barH:      {label: 'Barras horizontales'},
        pie:       {label: 'Pastel'},
        doughnut:  {label: 'Dona'},
        line:      {label: 'Línea'}
    };
    var REPORTS = [
        {id: 'medicos',      endpoint: 'medicos',      def: 'barH',     allowed: ['bar', 'barH', 'pie', 'doughnut']},
        {id: 'dias',         endpoint: 'dias',         def: 'bar',      allowed: ['bar', 'barH', 'line', 'pie', 'doughnut']},
        {id: 'medicamentos', endpoint: 'medicamentos', def: 'barH',     allowed: ['bar', 'barH', 'pie', 'doughnut']},
        {id: 'pagos',        endpoint: 'pagos',        def: 'pie',      allowed: ['pie', 'doughnut', 'bar', 'barH']}
    ];

    var root = document.getElementById('reportsRoot');
    var apiBase = root.getAttribute('data-api');
    var form = document.getElementById('reportFilters');
    var globalMsg = document.getElementById('reportMsg');
    var state = {}; // id -> {chart, data, type}

    function storeGet(key) { try { return window.localStorage.getItem(key); } catch (e) { return null; } }
    function storeSet(key, val) { try { window.localStorage.setItem(key, val); } catch (e) { /* sin almacenamiento: no pasa nada */ } }

    function buildQuery(report) {
        var p = new URLSearchParams();
        ['desde', 'hasta', 'sucursalId'].forEach(function (n) {
            var v = form.elements[n].value;
            if (v) p.set(n, v);
        });
        if (report.id === 'medicos' || report.id === 'medicamentos') p.set('top', form.elements.top.value);
        if (report.id === 'pagos') p.set('fuente', form.elements.fuente.value);
        return p.toString();
    }

    function show(el, on) { el.classList.toggle('hidden', !on); }

    function datasetColors(n) {
        var out = [];
        for (var i = 0; i < n; i++) out.push(COLORS[i % COLORS.length]);
        return out;
    }

    function chartConfig(type, data) {
        var labels = data.points.map(function (p) { return p.label; });
        var values = data.points.map(function (p) { return p.value; });
        var circular = type === 'pie' || type === 'doughnut';
        var cfg = {
            type: type === 'barH' ? 'bar' : type,
            data: {
                labels: labels,
                datasets: [{
                    label: data.metricLabel,
                    data: values,
                    backgroundColor: circular || type === 'bar' || type === 'barH' ? datasetColors(values.length) : 'rgba(15,76,129,0.15)',
                    borderColor: type === 'line' ? '#0f4c81' : '#ffffff',
                    borderWidth: type === 'line' ? 3 : 1,
                    fill: type === 'line',
                    tension: 0.25,
                    pointRadius: type === 'line' ? 4 : 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: type === 'barH' ? 'y' : 'x',
                plugins: {
                    legend: {display: circular, position: 'bottom'},
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                var v = ctx.parsed && typeof ctx.parsed === 'object' ?
                                    (type === 'barH' ? ctx.parsed.x : ctx.parsed.y) : ctx.parsed;
                                var pt = data.points[ctx.dataIndex];
                                var pct = data.total > 0 ? ' (' + (pt.value * 100 / data.total).toFixed(1) + '%)' : '';
                                return ' ' + data.metricLabel + ': ' + v + pct;
                            }
                        }
                    }
                }
            }
        };
        if (!circular) {
            var valueAxis = {beginAtZero: true, ticks: {precision: 0}};
            cfg.options.scales = type === 'barH' ? {x: valueAxis} : {y: valueAxis};
        }
        return cfg;
    }

    function renderChart(report) {
        var st = state[report.id];
        var canvas = document.getElementById('chart-' + report.id);
        if (st.chart) { st.chart.destroy(); st.chart = null; }
        if (!st.data || st.data.points.length === 0) return;
        st.chart = new Chart(canvas.getContext('2d'), chartConfig(st.type, st.data));
    }

    function renderTable(report) {
        var data = state[report.id].data;
        var table = document.getElementById('table-' + report.id);
        table.textContent = '';
        if (!data || data.points.length === 0) return;
        var head = table.createTHead().insertRow();
        ['#', 'Descripción', data.metricLabel, '%'].concat(data.extraLabel ? [data.extraLabel] : []).forEach(function (h) {
            var th = document.createElement('th');
            th.textContent = h;
            head.appendChild(th);
        });
        var body = table.createTBody();
        data.points.forEach(function (p, i) {
            var row = body.insertRow();
            var cells = [String(i + 1), p.label, String(p.value),
                data.total > 0 ? (p.value * 100 / data.total).toFixed(1) + '%' : '0%']
                .concat(data.extraLabel ? [p.extra || ''] : []);
            cells.forEach(function (c) { row.insertCell().textContent = c; });
        });
        var foot = table.createTFoot().insertRow();
        var tdLabel = foot.insertCell();
        tdLabel.colSpan = 2;
        tdLabel.textContent = 'Total';
        foot.insertCell().textContent = String(data.total);
        foot.insertCell().textContent = '';
        if (data.extraLabel) foot.insertCell().textContent = '';
    }

    function load(report) {
        var card = document.getElementById('card-' + report.id);
        var status = card.querySelector('.report-status');
        var wrap = card.querySelector('.chart-wrap');
        status.textContent = 'Cargando…';
        status.className = 'report-status';
        show(status, true);
        return fetch(apiBase + '/' + report.endpoint + '?' + buildQuery(report), {credentials: 'same-origin'})
            .then(function (res) {
                return res.json().catch(function () { return {}; }).then(function (body) {
                    if (!res.ok) throw new Error(body.error || 'No se pudo cargar el reporte.');
                    return body;
                });
            })
            .then(function (data) {
                state[report.id].data = data;
                var empty = data.points.length === 0;
                show(wrap, !empty);
                if (empty) {
                    status.textContent = 'No hay datos para los filtros seleccionados.';
                    status.className = 'report-status report-empty';
                } else {
                    show(status, false);
                }
                renderChart(report);
                renderTable(report);
            })
            .catch(function (err) {
                state[report.id].data = null;
                show(wrap, false);
                status.textContent = err.message;
                status.className = 'report-status error-text';
                show(status, true);
                renderChart(report);
                renderTable(report);
            });
    }

    function loadAll() {
        show(globalMsg, false);
        var from = form.elements.desde.value, to = form.elements.hasta.value;
        if (from && to && from > to) {
            globalMsg.textContent = 'La fecha inicial no puede ser posterior a la fecha final.';
            show(globalMsg, true);
            return;
        }
        REPORTS.forEach(load);
    }

    function setup(report) {
        var card = document.getElementById('card-' + report.id);
        var select = card.querySelector('.chart-type');
        var saved = storeGet('reportes.tipo.' + report.id);
        var type = report.allowed.indexOf(saved) >= 0 ? saved : report.def;
        state[report.id] = {chart: null, data: null, type: type};

        report.allowed.forEach(function (t) {
            var opt = document.createElement('option');
            opt.value = t;
            opt.textContent = TYPES[t].label;
            select.appendChild(opt);
        });
        select.value = type;
        select.addEventListener('change', function () {
            state[report.id].type = select.value;
            storeSet('reportes.tipo.' + report.id, select.value);
            renderChart(report);
        });

        card.querySelector('.chart-download').addEventListener('click', function () {
            var st = state[report.id];
            if (!st.chart) return;
            // Fondo blanco: el canvas es transparente y el PNG se vería negro en algunos visores.
            var src = document.getElementById('chart-' + report.id);
            var tmp = document.createElement('canvas');
            tmp.width = src.width; tmp.height = src.height;
            var ctx = tmp.getContext('2d');
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, tmp.width, tmp.height);
            ctx.drawImage(src, 0, 0);
            var a = document.createElement('a');
            a.href = tmp.toDataURL('image/png');
            a.download = 'reporte-' + report.id + '.png';
            a.click();
        });
    }

    if (typeof Chart === 'undefined') {
        globalMsg.textContent = 'No se pudo cargar la librería de gráficos (js/chart.umd.min.js).';
        show(globalMsg, true);
        return;
    }
    REPORTS.forEach(setup);
    form.addEventListener('submit', function (ev) { ev.preventDefault(); loadAll(); });
    document.getElementById('reportReset').addEventListener('click', function () {
        form.reset();
        loadAll();
    });
    loadAll();
})();
