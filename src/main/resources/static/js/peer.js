/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree.
 */

'use strict';

const websocketAddr = document.getElementById('websocketAddr');
const startButton = document.getElementById('startButton');
const hangupButton = document.getElementById('hangupButton');
hangupButton.disabled = true;
startButton.addEventListener('click', start);
hangupButton.addEventListener('click', hangup);


websocketAddr.value = 'ws://' + location.host + '/channels/TEST-001';


let startTime;
const localVideo = document.getElementById('localVideo');
const remoteVideo = document.getElementById('remoteVideo');

localVideo.addEventListener('loadedmetadata', function() {
    console.log(`Local video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});

remoteVideo.addEventListener('loadedmetadata', function() {
    console.log(`Remote video videoWidth: ${this.videoWidth}px,  videoHeight: ${this.videoHeight}px`);
});

remoteVideo.addEventListener('resize', () => {
    console.log(`Remote video size changed to ${remoteVideo.videoWidth}x${remoteVideo.videoHeight}`);
// We'll use the first onsize callback as an indication that video has started
// playing out.
if (startTime) {
    const elapsedTime = window.performance.now() - startTime;
    console.log('Setup time: ' + elapsedTime.toFixed(3) + 'ms');
    startTime = null;
}
});

// websocket controller
const webSocketCtrl = (function(){

    let $self = {};

    $self.ws = undefined;

    $self.open = function(url, messageHandler) {
        $self.ws = new WebSocket(url);

        $self.ws.onclose = function(e){
            messageHandler('onclose', e);
        }

        $self.ws.onerror = function(e){
            messageHandler('onerror', e);
        }

        $self.ws.onmessage = function(event){
            messageHandler('onmessage', event.data);
        }

        $self.ws.onopen = function(event){
            messageHandler('onopen', event);
        }
    }

    $self.send = function(data){
        console.log('ws send : ', data);
        $self.ws.send(JSON.stringify(data));
    }

    return $self;
})();


async function websocketHandler(eventType , data){

    if(eventType === 'onerror') {
        console.error('websocket Error' , data);
        alert('websocket Error');
    }

    if(eventType === 'onclose') {
        console.log('websocket Closed' , data);
        alert('websocket Closed');
    }

    if(eventType === 'onopen') {
        console.log('websocket Opened' , data);
        streamOpen()
    }

    if(eventType === 'onmessage') {
        console.log('websocket onmessage' , data);
        var dataObj = JSON.parse(data);

        if(dataObj.type == 'CHANNEL_JOIN') {
            createOffer();
        }

        if(dataObj.type == 'RTC_OFFER') {
            var desc = JSON.parse(dataObj.messageBody);
            receiveOffer(desc);
        }

        if(dataObj.type == 'RTC_ANSWER') {
            var desc = JSON.parse(dataObj.messageBody);
            receiveAnswer(desc);
        }

        if(dataObj.type == 'RTC_ICE_CANDIDATE') {
            var candidate = JSON.parse(dataObj.messageBody);
            receiveIceCandidate(candidate);
        }
    }
}

// localStream 변수
let localStream;



// 시작버튼
async function start() {
    console.log('localStream 을 시작합니다.');
    websocketAddr.disabled = true;
    startButton.disabled = true;

    try {

        // websocket channel 에 연결합니다.
        if(!websocketAddr.value) {
            alert('invalid websocket addr');
        }

        webSocketCtrl.open(websocketAddr.value , websocketHandler);
    } catch (e) {
        alert(`getUserMedia() error: ${e.name}`);
    }
}

async function streamOpen(){
    try {
        const stream = await navigator.mediaDevices.getUserMedia({audio: true, video: true});
        console.log('local stream 획득');
        // local Video Elm 에 localStream 주입
        localVideo.srcObject = stream;
        localStream = stream;


        hangupButton.disabled = false;
        console.log('Starting call');
        startTime = window.performance.now();
        const videoTracks = localStream.getVideoTracks();
        const audioTracks = localStream.getAudioTracks();

        if (videoTracks.length > 0) {
            console.log(`Using video device: ${videoTracks[0].label}`);
        }
        if (audioTracks.length > 0) {
            console.log(`Using audio device: ${audioTracks[0].label}`);
        }

        const configuration = getSelectedSdpSemantics();

        configuration.iceServers = [{
            urls: "stun:stun.l.google.com:19302"
        }];

        localPeerConnection = new RTCPeerConnection(configuration);
        localPeerConnection.addEventListener('icecandidate', e => onIceCandidate(e));
        localPeerConnection.addEventListener('iceconnectionstatechange', e => onIceStateChange(e));

        localPeerConnection.addEventListener('track', function (e) {
            console.log('track', e);
            if (remoteVideo.srcObject !== e.streams[0]) {
                remoteVideo.srcObject = e.streams[0];
                console.log('received remote stream');
            }
        });

        localStream.getTracks().forEach(track => localPeerConnection.addTrack(track, localStream));

        // open and channel join
        webSocketCtrl.send({type: 'CHANNEL_JOIN', messageBody: ''})
        
    } catch (e) {
        alert('error :' + e);
    }
}





let localPeerConnection;
let remotePeerConnection;

const offerOptions = {
    offerToReceiveAudio: 1,
    offerToReceiveVideo: 1
};



function getSelectedSdpSemantics() {
    const sdpSemanticsSelect = document.querySelector('#sdpSemantics');
    const option = sdpSemanticsSelect.options[sdpSemanticsSelect.selectedIndex];
    return option.value === '' ? {} : {sdpSemantics: option.value};
}

async function createOffer() {

    try {
        console.log('localPeerConnection createOffer start');
        const desc = await localPeerConnection.createOffer(offerOptions);
        await asyncLog('localPeerConnection createOffer complete');
        await asyncLog('localPeerConnection setLocalDescription start');
        await localPeerConnection.setLocalDescription(desc);
        await asyncLog('localPeerConnection setLocalDescription complete');
        await webSocketCtrl.send({type:'RTC_OFFER', messageBody : JSON.stringify(desc)})
        await asyncLog('localPeerConnection send RTC_OFFER complete');
    } catch (e) {
        console.error(`Failed to create session description: ${e}`);
    }
}

async function receiveOffer(desc) {
    console.log('localPeerConnection receive offer');
    console.log('localPeerConnection setRemoteDescription start');
    try {
        await localPeerConnection.setRemoteDescription(desc);
        await asyncLog('localPeerConnection setRemoteDescription complete');
        await createAnswer();
    } catch (e) {
        console.error(`Failed to set session description: ${e}`);
    }
}

async function asyncLog(message){
    console.log(message);
}

async function createAnswer(){
    try {
        console.log('localPeerConnection createAnswer start');
        const desc = await localPeerConnection.createAnswer();
        await asyncLog('localPeerConnection createAnswer complete');
        await asyncLog('localPeerConnection setLocalDescription start');
        await localPeerConnection.setLocalDescription(desc);
        await asyncLog('localPeerConnection setLocalDescription complete');
        await webSocketCtrl.send({type:'RTC_ANSWER', messageBody : JSON.stringify(desc)})
    } catch (e) {
        console.error(`Failed to set session description: ${e}`);
    }
}

async function receiveAnswer(desc){
    console.log('localPeerConnection receive answer');
    console.log('localPeerConnection setRemoteDescription start');
    try {
        await localPeerConnection.setRemoteDescription(desc);
        await asyncLog('localPeerConnection setRemoteDescription complete');
    } catch (e) {
        console.error(`Failed to set session description: ${e}`);
    }
}


async function onIceCandidate(e) {

    console.log('onIceCandidate' , e.candidate);
    webSocketCtrl.send({type:'RTC_ICE_CANDIDATE', messageBody : JSON.stringify(e.candidate)})
}

async function onIceStateChange(e) {
    console.log('onIceStateChange' , e);
}

var candidateBuf = [];

async function receiveIceCandidate(candidate) {

    if(!localPeerConnection && candidate) {
        candidateBuf.push(candidate)
    }

    try {
        await localPeerConnection.addIceCandidate(candidate);
    } catch (e) {
        console.error('failed to add ICE Candidate:', e);
    }
}

function hangup() {
    console.log('Ending call');
    localPeerConnection.close();
    hangupButton.disabled = true;
}