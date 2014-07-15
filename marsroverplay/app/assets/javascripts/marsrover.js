 var host = window.location.host;
 var wsUri = "ws://" + host + "/";
 var websocket = {};

 var width = {};
 var height = {};
 var size_x = {};
 var size_y = {};

 var STATUS_ID ="#status";

 var rover_img_src = {};
 var facingPics = {};

 window.onbeforeunload = function () {
     websocket.onclose = function () {}; // disable onclose handler first
     websocket.close();
 };

$(document).ready(function(){

    websocket = new WebSocket(wsUri + "startdisplay");

    websocket.onopen = function (evt) {
     onOpen(evt);
    };
    websocket.onclose = function (evt) {
        onClose(evt);
    };
    websocket.onmessage = function (evt) {
        onMessage(evt);
    };
});

function onOpen(evt) {
     writeToScreen("CONNECTED");

 }

 function onClose(evt) {
     writeToScreen("DISCONNECTED" + evt.code + evt.reason);
 }

 function onMessage(evt) {
     var result;
     try{
        result = JSON.parse(evt.data);
        handle(result);
     }catch(exception){
     }
     writeToScreen(evt.data);
 }

 function handle(json){
     if(json.hasOwnProperty('config')) handleConfig(json.config);
     else if(json.hasOwnProperty('state')) {
         $.each(json.state, function( index, state ) {
           move(state.name, state.x, state.y, state.facing);
         });
     } else if(json.hasOwnProperty('final')) {
        $.each(json.final, function( index, state ) {
          finalState(state.name, state.x, state.y, state.facing);
        });
     } else if(json.hasOwnProperty('collusions')) {
        $.each(json.collusions, function( index, position ) {
          collusions(position.x, position.y);
        });
     }
 }

function handleConfig(configuration){

   size_x = configuration.x+1;
   size_y = configuration.y+1;

   width = 1200 / size_x;
   height = 600 / size_y;
   for (var i = 1; i < size_x ; i++) {
    paper.path("M{0},0L{1},600".format(width*i, width*i));
   }

   for (var j = 1; j < size_y ; j++) {
    paper.path("M0,{0}L1200,{1}".format(height*j, height*j));
   }

   paper.text(calculateCenterXOf(0), calculateCenterYOf(0), '0,0').attr({'font-size': 24});
   paper.text(calculateCenterXOf(configuration.x), calculateCenterYOf(configuration.y), configuration.x + "," + configuration.y).attr({'font-size': 24});
 }

 function calculateCenterXOf(position){
    return (width*(parseInt(position)))+width/2;
 }

 function calculateCenterYOf(position){
     return (height*(size_y-parseInt(position)))-height/2;
 }

 function writeToScreen(message) {
     var pre = document.createElement("p");
     pre.style.wordWrap = "break-word";
     pre.innerHTML = message + " " + new Date();
     $(STATUS_ID).append(pre);
 }

 // First, checks if it isn't implemented yet.
 if (!String.prototype.format) {
   String.prototype.format = function() {
     var args = arguments;
     return this.replace(/{(\d+)}/g, function(match, number) {
       return typeof args[number] != 'undefined' ? args[number] : match;
     });
   };
 }

 function finalState(name, x, y, facing) {
       var text = paper.text(calculateCenterXOf(x), calculateCenterYOf(y), name+" - "+x+" - "+y+" - "+facing).attr({'fill': 'red','font-size': 26, "font-weight": "bold"});
       var box = text.getBBox();
       var rect = paper.rect(box.x, box.y, box.width, box.height).attr('fill', 'white');
       text.toFront();
 }

 function collusions(x, y) {
       paper.image(explosion_img_src, calculateCenterXOf(x)-75, calculateCenterYOf(y)-75, 150, 150);
 }

function move(name, x, y, facing){

    var moveX = calculateCenterXOf(x)-55;
    var moveY = calculateCenterYOf(y)-55;

    var exists = paper.getById(name+"_rover");
    var speed = 1000;
    if(exists === null){
     createRover(name, facing);
     speed = 0;
    }
    rotate(name, facing);

    paper.getById(name+"_rover").animate({"x": moveX , "y":moveY}, speed);
    paper.getById(name+"_arrow").animate({"x": moveX , "y":moveY}, speed);
    paper.getById(name+"_text").animate({"x": moveX+50 , "y": moveY+10}, speed);
}
function rotate(name, facing){
    var f = paper.getById(name+"_arrow").data('facing');
    if(f != facing){
        paper.getById(name+"_arrow").attr({src: facingPics[facing]}).data('facing', facing);
        paper.getById(name+"_arrow").animate({'transform':"s2"}, 1000, "bounce", function(){
            paper.getById(name+"_arrow").animate({'transform':"s1"}, 1000, "bounce");
        });
    }
}
function createRover(name, facing){
    var rover = paper.image(rover_img_src, 0, 0, 110, 110);
    rover.id = name+"_rover";

    var arrow = paper.image(facingPics[facing], 0, 0, 30,30);
    arrow.id = name+"_arrow";
    arrow.data('facing', facing);

    var text = paper.text(0, 0, name);
    text.id = name + "_text";
}


