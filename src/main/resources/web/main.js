var persistentState;
let states = {Questions: 0, Correcting: 1, Feedback: 2, NoQuestions: 4};
Object.freeze(states);
var systemState = states.NoQuestions;

function createActivationSession() {
    const myBody = {
        activation: true,
        maxCount: 10
    };
    console.log("Invoking session creation.")
    getPOSTResponse("/api/session", myBody).then(displayState);
}

function createTrainingSession() {
    const myBody = {
        activation: false,
        maxCount: 10
    };
    console.log("Invoking session creation.")
    getPOSTResponse("/api/session", myBody).then(displayState);
}

function onAnswer() { //Handles button presses from button 'answer'
    console.log("Posting answer: " + userAnswer.value)
    getPOSTResponseRawIn("/api/answer", userAnswer.value, 'text/plain').then(displayState);
}

function onCorrect() { //Handles button presses from button 'correct'
    console.log("Submitting correction: " + true)
    getPOSTResponse("/api/correction", true).then(displayState);
}

function onIncorrect() { //Handles button presses from button 'incorrect'
    if (systemState == states.Correcting) {
        displayState(persistentState);
    } else {
        console.log("Submitting correction: " + false)
        getPOSTResponse("/api/correction", false).then(displayState)
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
        session.disabled = false;
        activate.disabled = false;
        userAnswer.disabled = true;
        answer.disabled = true
        correct.disabled = true;
        incorrect.disabled = true;

        solution.innerHTML = "";
        question.innerHTML = "";
        info.innerHTML = "";
        userAnswer.value = "";
    } else if (systemState == states.Questions) {
        session.disabled = true;
        activate.disabled = true;
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
        info.innerHTML = "Remaining: " + state.remainingQuestions;
    } else if (systemState == states.Correcting) {
        session.disabled = true;
        activate.disabled = true;
        userAnswer.disabled = true;
        answer.disabled = true;
        correct.disabled = true;
        incorrect.disabled = false;

        solution.innerHTML = state.lastResult.solution;
    } else if (systemState == states.Feedback) {
        session.disabled = true;
        activate.disabled = true;
        userAnswer.disabled = true;
        answer.disabled = true;
        correct.disabled = false;
        incorrect.disabled = false;

        solution.innerHTML = state.lastResult.solution;
    }
}

async function getPOSTResponseRawIn(path, rawIn, type) {
    let response = fetch(path, {
        method: 'POST',
        body: rawIn,
        headers:{
          'Content-Type': type
        }
    });
    return response.then(result => result.json());
}

async function getPOSTResponse(path, parameters) {
    return getPOSTResponseRawIn(path, JSON.stringify(parameters), 'application/json')
}