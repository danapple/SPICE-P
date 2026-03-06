let update_message = document.getElementById('update_message');
let symbol_input = document.getElementById('symbol_input');
let value_input = document.getElementById('value_input');
let quantity_input = document.getElementById('quantity_input');
let simulation_date = document.getElementById('simulation_date');

let simulation_date_display = document.getElementById('simulation_date_display');
let simulation_total = document.getElementById('simulation_total');
let best_asset = document.getElementById('best_asset');
let worst_asset = document.getElementById('worst_asset');

let display_timeout = undefined;

let assets = [];

let assetsTable = new DataTable('#assets_table', {
    columns: [
        {title: 'Symbol', data: 'symbol', name: 'symbol', className: 'dt-right', visible: true},
        {title: 'Quantity', data: 'quantity', name: 'quantity', className: 'dt-right', visible: true},
        {title: 'Original Value', data: 'value', name: 'value', className: 'dt-right', visible: true},
        {title: 'Actions', data: 'actions', name: 'actions', className: 'dt-right', visible: true, orderable: false},
    ],
    rowId: 'row_id',
    scrollY: true,
    paging: false,
    order: [[0, 'asc']],
    info: false,
    scrollX: true,
});

function renderTwoDecimals(num) {
    return num.toLocaleString('en', {
        useGrouping: true, minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}

function renderCurrency(num) {
    return "$" + renderTwoDecimals(num);
}

function renderPercentage(num) {
    return renderTwoDecimals(num) + "%";
}

function makeXhttp(method, path, callback) {
    let xhttp = new XMLHttpRequest();
    if (callback !== undefined && callback !== null) {
        xhttp.onreadystatechange = function () {
            if (this.readyState === 4) {
                if (this.status < 400) {
                    let responseBody = {}
                    if (this.responseText.length > 0) {
                        responseBody = JSON.parse(this.responseText)
                    }
                    callback(this.status, responseBody);
                }
                else {
                    displayMessage(this.responseText);
                }
            }
        };
    }
    xhttp.open(method, path, true);
    xhttp.setRequestHeader("Content-type", "application/json");
    return xhttp;
}

function displayMessage(message) {
    if (display_timeout !== undefined) {
        clearTimeout(display_timeout);
    }
    update_message.innerHTML = "<b>" + message + "</b>";
    display_timeout = setTimeout(clearMessage, 10000)
}

function clearMessage() {
    update_message.innerHTML = "<br>";
}

function addAsset() {
    let symbol = symbol_input.value.trim();
    let value = parseFloat(value_input.value);
    let quantity = parseFloat(quantity_input.value);

    if (!symbol || isNaN(value) || isNaN(quantity)) {
        displayMessage("Please enter valid symbol, value, and quantity");
        return;
    }

    // Check if asset already exists
    let existingIndex = assets.findIndex(a => a.symbol === symbol);
    if (existingIndex >= 0) {
        // Update existing asset
        assets[existingIndex].value = value;
        assets[existingIndex].quantity = quantity;
    } else {
        // Add new asset
        assets.push({
            symbol: symbol,
            value: value,
            quantity: quantity
        });
    }

    refreshAssetsTable();

    // Clear inputs
    symbol_input.value = "";
    value_input.value = "";
    quantity_input.value = "";

    displayMessage("Asset added: " + symbol);
}

function removeAsset(symbol) {
    assets = assets.filter(a => a.symbol !== symbol);
    refreshAssetsTable();
    displayMessage("Asset removed: " + symbol);
}

function refreshAssetsTable() {
    assetsTable.clear();

    for (let i = 0; i < assets.length; i++) {
        let asset = assets[i];
        assetsTable.row.add({
            symbol: asset.symbol,
            quantity: asset.quantity,
            value: renderCurrency(asset.value),
            actions: '<button onclick="removeAsset(\'' + asset.symbol + '\')">Remove</button>',
            row_id: 'asset_' + i
        });
    }

    assetsTable.draw();
}

function clearAllAssets() {
    assets = [];
    refreshAssetsTable();
    simulation_date_display.innerHTML = "";
    simulation_total.innerHTML = "";
    best_asset.innerHTML = "";
    worst_asset.innerHTML = "";
    displayMessage("All assets cleared");
}

function runSimulation() {
    if (assets.length === 0) {
        displayMessage("Please add at least one asset to simulate");
        return;
    }

    let xhttp = makeXhttp("POST", "/simulate", (status, body) => {
        displaySimulationResults(body);
    });

    // Convert assets to the format expected by the API
    let requestAssets = assets.map(a => ({
        symbol: a.symbol,
        value: a.value,
        quantity: a.quantity
    }));

    let requestBody = {
        assets: requestAssets
    };

    // Add simulation date if provided (convert to YYYY-MM-DD format)
    if (simulation_date.value) {
        let dateObj = $('#simulation_date').datepicker('getDate');
        if (dateObj) {
            let year = dateObj.getFullYear();
            let month = String(dateObj.getMonth() + 1).padStart(2, '0');
            let day = String(dateObj.getDate()).padStart(2, '0');
            requestBody.date = year + '-' + month + '-' + day;
        }
    }

    xhttp.send(JSON.stringify(requestBody));
    displayMessage("Running simulation...");
}

function displaySimulationResults(results) {
    // Display date in dd MMM yyyy format
    if (results.date) {
        let dateParts = results.date.split('-');
        if (dateParts.length === 3) {
            let dateObj = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);
            let monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
                            'July', 'August', 'September', 'October', 'November', 'December'];
            simulation_date_display.innerHTML =
                String(dateObj.getDate()).padStart(2, '0') + ' ' +
                monthNames[dateObj.getMonth()] + ' ' +
                dateObj.getFullYear();
        } else {
            simulation_date_display.innerHTML = results.date;
        }
    }

    simulation_total.innerHTML = renderCurrency(results.total);

    if (results.best_asset) {
        let bestColor = results.best_performance >= 0 ? 'var(--color-success)' : 'var(--color-danger)';
        best_asset.innerHTML = results.best_asset + "<br>" +
            "<span style='color: " + bestColor + ";'>" +
            renderPercentage(results.best_performance) + "</span>";
    }

    if (results.worst_asset) {
        let worstColor = results.worst_performance >= 0 ? 'var(--color-success)' : 'var(--color-danger)';
        worst_asset.innerHTML = results.worst_asset + "<br>" +
            "<span style='color: " + worstColor + ";'>" +
            renderPercentage(results.worst_performance) + "</span>";
    }

    displayMessage("Simulation complete");
}

document.getElementById('add_asset_button').addEventListener('click', addAsset);
document.getElementById('simulate_button').addEventListener('click', runSimulation);
document.getElementById('clear_button').addEventListener('click', clearAllAssets);

// Allow Enter key to add asset
symbol_input.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') addAsset();
});
value_input.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') addAsset();
});
quantity_input.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') addAsset();
});

// Initialize datepicker with dd MMM yyyy format
$('#simulation_date').datepicker({
    dateFormat: 'dd MM yy',
    changeMonth: true,
    changeYear: true,
    yearRange: '2008:' + new Date().getFullYear(),
    maxDate: 0,  // Prevent future dates (0 = today)
    showButtonPanel: false,
    monthNamesShort: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
});

displayMessage("Welcome to Wallet Profit Simulation - Add assets to get started");
