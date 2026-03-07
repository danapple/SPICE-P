let tokenKey;
let update_message = document.getElementById('update_message');
let wallet_title = document.getElementById('wallet_title');
let wallet_email_address = document.getElementById('wallet_email_address');

let symbol_input = document.getElementById('symbol_input');
let price_input = document.getElementById('price_input');
let quantity_input = document.getElementById('quantity_input');

let wallet_total = document.getElementById('wallet_total');

let wallet_display_timeout = undefined;

let positionsTable = new DataTable('#positions_table', {
        columns: [
            {title: 'SortSymbol', data: 'sortSymbol', name: 'sortSymbol', className: 'dt-right', visible: false},
            {title: 'Symbol', data: 'symbol', name: 'symbol', className: 'dt-right', visible: true},
            {title: 'Quantity', data: 'quantity', name: 'quantity', className: 'dt-right', visible: true},
            {title: 'Price', data: 'price', name: 'price', className: 'dt-right', visible: true},
            {title: 'Cost', data: 'cost', name: 'cost', className: 'dt-right', visible: true},
            {title: 'Mark', data: 'mark', name: 'mark', className: 'dt-right', visible: true},
            {title: 'Value', data: 'value', name: 'value', className: 'dt-right', visible: true},
            {title: 'Open Gain', data: 'openGain', name: 'openGain', className: 'dt-right', visible: true},
            {title: 'Closed Gain', data: 'closedGain', name: 'closedGain', className: 'dt-right', visible: true},
           ],
        rowId: 'row_id',
        scrollY: true, // Placeholder: initializes scroll DOM, CSS handles actual height
//        scrollCollapse: true, // Crucial: Forces table to fill the flex container
//        scrollResize: true,
        paging: false,
        order: [[0, 'asc']],
        fixedColumns: {
            start: 0
        },
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
    if (wallet_display_timeout !== undefined) {
       clearTimeout(wallet_display_timeout);
    }
    update_message.innerHTML = "<b>" + message + "</b>";
    wallet_display_timeout = setTimeout(clearMessage, 10000)
}

function clearMessage() {
   update_message.innerHTML = "<br>";
}

function displayWalletInfo(walletInfo, existing) {
    if (wallet_display_timeout !== undefined) {
        clearTimeout(wallet_display_timeout);
    }
    wallet_email_address.value = "";
    tokenKey = walletInfo.id;
    if (existing === true) {
        displayMessage("Loaded existing wallet " + walletInfo.emailAddress);
    }
    else {
        displayMessage("Created new wallet " + walletInfo.emailAddress);
    }
    refreshWallet();
    wallet_title.innerHTML = walletInfo.emailAddress;
}

function loadWallet() {
//    console.log("Load wallet");
    wallet_total.innerHTML = "";
    wallet_title.innerHTML = "";
    positionsTable.clear();

    tokenKey = undefined;

    let xhttp = makeXhttp("POST", "/wallets", (status, body) => {
//        console.log(status + "got wallet " + body);
        let existing = false;
        if (status === 200) {
            existing = true;
        }
        displayWalletInfo(body, existing);
    });

    let body = JSON.stringify({
        emailAddress: wallet_email_address.value
    });
    xhttp.send(body);
}

function addAsset() {
//    console.log("Add asset");
    if (tokenKey === undefined) {
        return;
    }
    let xhttp = makeXhttp("POST", "/wallets/" + tokenKey + "/assets", (status, body) => {
        symbol_input.value = "";
        price_input.value = "";
        quantity_input.value = "";
        refreshWallet();
    });

    let body = JSON.stringify({
        symbol: symbol_input.value,
        price: price_input.value,
        quantity: quantity_input.value
    });
    xhttp.send(body);
}

function displayPositions(wallet_data) {
    if (tokenKey !== wallet_data['id']) {
        return;
    }
    wallet_total.innerHTML = renderCurrency(wallet_data['total']);
    var assetCount = wallet_data.assets.length;
    positionsTable.clear();
    let totalCost = 0;
    let totalValue = 0;
    let totalOpenGain = 0;
    let totalClosedGain = 0;

    for (var i = 0; i < assetCount; i++) {
        let asset = wallet_data.assets[i];
        totalCost += asset['cost'];
        totalValue += asset['value'];
        totalOpenGain += asset['openGain'];
        totalClosedGain += asset['closedGain'];
        positionsTable.row.add({
                    sortSymbol: asset['symbol'],
                    symbol: asset['symbol'],
                    quantity: asset['quantity'],
                    price: renderCurrency(asset['price']),
                    cost: renderCurrency(asset['cost']),
                    mark: renderCurrency(asset['mark']),
                    value: renderCurrency(asset['value']),
                    openGain: renderCurrency(asset['openGain']),
                    closedGain: renderCurrency(asset['closedGain']),
                });

    }
    positionsTable.row.add({
                    sortSymbol: 'zzz',
                    symbol: '-Total-',
                    quantity: '-',
                    price: '-',
                    cost: renderCurrency(totalCost),
                    mark: '-',
                    value: renderCurrency(totalValue),
                    openGain: renderCurrency(totalOpenGain),
                    closedGain: renderCurrency(totalClosedGain),
    });
    positionsTable.draw();
}

function refreshWallet() {
    if (tokenKey === undefined) {
//        console.log("No wallet to refresh");
        return;
    }
//    console.log("Refresh wallet " + tokenKey);
    let xhttp = makeXhttp("GET", "/wallets/" + tokenKey, (status, responseBody) => {
         displayPositions(responseBody);
    });
    xhttp.send();
}

document.getElementById('load_wallet_button').addEventListener('click', loadWallet);

document.getElementById('add_asset_button').addEventListener('click', addAsset);

setInterval(refreshWallet, 9500)

displayMessage("Welcome to Dan Applebaum's SPICE-P implementation");
