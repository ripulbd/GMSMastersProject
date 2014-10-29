/**
 * New node file
 */
$(document).ready(function() {

//Other code ...

/*HEIGHT and WIDTH below is your screen size not the your browser's
inner Size. AND if your width is 1280px then replace WIDTH by 1280.0 
(Note 0 after point)in the code below and same for HEIGHT.*/

factor_x = ($(window).width()) / WIDTH;
factor_y = ($(window).height( ))/HEIGHT;

$('html').css("transform","scale("+factor_x+","+factor_y+")");  
//For Firefox
$('html').css("-moz-transform","scale("+factor_x+","+factor_y+")"); 
      
//For Google Chrome
$('html').css("-webkit-transform","scale("+factor_x+","+factor_y+")");

//For Opera
$('html').css("-o-transform","scale("+factor_x+","+factor_y+")");

});