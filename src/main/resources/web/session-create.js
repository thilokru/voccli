var tree;

function onLoad() {
    fetch("/api/v1/overview").then(result => {
        tree = result.json();
        for (var key in tree) {
            if (tree.hasOwnProperty(key)) {
                var option = document.createElement("option");
                option.text = key;
                option.value = key;
                newLang.add(option);
                var option2 = document.createElement("option");
                option2.text = key;
                option2.value = key;
                continueLang.add(option);
            }
        }
    });
}


function langChange() {
    removeOptions(newBook);
    let selected = newLang.value;
    for (var key in tree[selected]) {
        if (tree[selected].hasOwnProperty(key)) {
            var option = document.createElement("option");
            option.text = key;
            option.value = key;
            newBook.add(option);
        }
    }
}

function bookChange() {
    removeOptions(newChapter);
    let lang = newLang.value;
    let book = newBook.value;
    for (var key in tree[lang][book]) {
        if (tree[lang][book].hasOwnProperty(key)) {
            var option = document.createElement("option");
            option.text = key;
            option.value = tree[lang][book][key]
            newChapter.add(option);
        }
    }
}

function onNewSession() {
    var qC = -1;
    var tmp = parseInt(newCount.value);
    if (tmp > 0) {
        qC = tmp
    }
    const myBody = {
        isActivation: false,
        maxCount: qC,
        selector: "sections: " + getSelectedValues(newChapter).join()
    };
    console.log("Invoking session creation.")
    getPOSTResponse("/api/v1/session", myBody).then(answer => window.location.href = "/session.html")
}

function onContinueSession() {
    var qC = -1;
    var tmp = parseInt(continueCount.value);
    if (tmp > 0) {
        qC = tmp
    }
    const myBody = {
        isActivation: false,
        maxCount: qC,
        selector: "lang: " + continueLang.value
    };
    console.log("Invoking session creation.")
    getPOSTResponse("/api/v1/session", myBody).then(answer => window.location.href = "/session.html")
}

async function getPOSTResponseRawIn(path, rawIn, type) {
    let response = fetch(path, {
        method: 'POST',
        body: rawIn,
        headers:{
          'Content-Type': type + "; charset=utf-8"
        }
    });
    return response.then(result => result.json());
}

async function getPOSTResponse(path, parameters) {
    return getPOSTResponseRawIn(path, JSON.stringify(parameters), 'application/json')
}

// Return an array of the selected opion values
// select is an HTML select element, stolen from StackOverflow
function getSelectValues(select) {
    var result = [];
    var options = select && select.options;
    var opt;

    for (var i=0, iLen=options.length; i<iLen; i++) {
        opt = options[i];

        if (opt.selected) {
            result.push(opt.value || opt.text);
        }
    }
    return result;
}

function clearSelected(select){
    var elements = select.options;

    for(var i = 0; i < elements.length; i++){
        elements[i].selected = false;
    }
}

function removeOptions(select){
    for(var i = select.options.length - 1 ; i >= 0 ; i--){
        select.remove(i);
    }
}