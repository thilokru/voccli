var persistentState;
let states = {Questions: 0, Correcting: 1, Feedback: 2, NoQuestions: 4};
Object.freeze(states);
var systemState = states.NoQuestions;
window.onunload = function() {
    getPOSTResponse("/api/v1/cancel", null);
}

function onCreateSession() {
    var qC = -1;
    var tmp = parseInt(questionCount.value);
    if (tmp > 0) {
        qC = tmp
    }
    const myBody = {
        isActivation: isActivation.checked,
        maxCount: qC
    };
    console.log("Invoking session creation.")
    getPOSTResponse("/api/v1/session", myBody).then(displayState);
}

function onAnswer() { //Handles button presses from button 'answer'
    console.log("Posting answer: " + userAnswer.value)
    getPOSTResponseRawIn("/api/v1/answer", userAnswer.value, 'text/plain').then(displayState);
}

function onCorrect() { //Handles button presses from button 'correct'
    console.log("Submitting correction: " + true)
    getPOSTResponse("/api/v1/correction", true).then(displayState);
}

function onIncorrect() { //Handles button presses from button 'incorrect'
    if (systemState == states.Correcting) {
        displayState(persistentState);
    } else {
        console.log("Submitting correction: " + false)
        getPOSTResponse("/api/v1/correction", false).then(displayState)
    }
}

function displayState(state) {
    persistentState = state;

    //DEBUG:
    console.log(state);
    console.log("Previous system state: " + systemState)

    //Advancing the state machine
    if (systemState == states.Questions) {
        if (state.lastResult != null && state.lastResult.type == "UNDETERMINED") {
            systemState = states.Feedback;
        } else if (state.lastResult != null && state.lastResult.type == "WRONG") {
            systemState = states.Correcting;
        } else if (state.remainingQuestions == 0) {
            systemState = states.NoQuestions;
        }
    } else if (systemState == states.Correcting) { //Calling this method again indicates the correction occurred.
        systemState = states.Questions;
    } else if (systemState == states.Feedback) { //Calling this method again indicates the correction occurred.
        systemState = states.Questions;
    } else if (systemState == states.NoQuestions) {
        if (state.remainingQuestions != 0) {
            systemState = states.Questions;
        } else {
            alert("You are done for today!")
        }
    }

    console.log("New system state: " + systemState)

    //Enable and disable UI state according to systemState
    if (systemState == states.NoQuestions) { //A session does not exist.
        disableForm(false)
        userAnswer.disabled = true;
        answer.disabled = true
        correct.disabled = true;
        incorrect.disabled = true;

        solution.innerHTML = "";
        question.innerHTML = "";
        info.innerHTML = "";
        remaining.innerHTML = "Remaining: 0";
        userAnswer.value = "";
    } else if (systemState == states.Questions) {
        disableForm(true)
        userAnswer.disabled = false;
        answer.disabled = false
        correct.disabled = true;
        incorrect.disabled = true;

        userAnswer.value = "";
        userAnswer.focus();
        question.innerHTML = state.currentQuestion.question;
        if (state.currentQuestion.solution != null) {
            solution.innerHTML = state.currentQuestion.solution;
        } else {
            solution.innerHTML = "";
        }
        var phase = state.currentQuestion.phase;
        var usage = state.currentQuestion.associatedData.usage;
        info.innerHTML = "Phase: " + phase + " Usage: " + usage;
        remaining.innerHTML = "Remaining: " + state.remainingQuestions;
    } else if (systemState == states.Correcting) {
        disableForm(true)
        userAnswer.disabled = true;
        answer.disabled = true;
        correct.disabled = true;
        incorrect.disabled = false;

        solution.innerHTML = state.lastResult.solution;
    } else if (systemState == states.Feedback) {
        disableForm(true)
        userAnswer.disabled = true;
        answer.disabled = true;
        correct.disabled = false;
        incorrect.disabled = false;

        solution.innerHTML = state.lastResult.solution;
    }
}

function disableForm(disabled) {
    questionCount.disabled = disabled
    isActivation.disabled = disabled
    createSession.disabled = disabled
    if (!disabled) {
        questionCount.value = "";
        isActivation.checked = false;
    }
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