
    var defaultTheme = 'default-black';
    var minFrameLength = 8;
    var pageurl = 'http://cortexit.org';

    var fontSize = 60;
    var text;
    var nodes = [];
    var currentNode;
    var speechEnabled = false;
    var prevID = null;
    var nextID = null;
    
    var widgets = { };
    
    function saveWidgets() {
        $.totalStorage('widgets', widgets);        
    }
    
    function enableVozmeSpeech(line) {
        speechEnabled = true;

        var speech = document.getElementById("_Speech");
        speech.style.display = 'inline';
        var speechLine = line.replace("&nbsp;", " ").replace(/<\/?[a-z][a-z0-9]*[^<>]*>/ig, "");

        //SEE: http://www.vikitech.com/980/top-10-web-based-services-for-text-to-speech-conversion

        var speechLineEncoded = escape(speechLine);
        //var speechURL = 'http://translate.google.com/translate_tts?q=' + speechLineEncoded;
        
        var speechURL = 'http://vozme.com/text2voice.php?bookmarklet=1&gn=fm&interface=full&default_language=en&text=' + speechLineEncoded;
        
        $('#_Speech').fadeIn('slow');

        speech.innerHTML = '<iframe src="' + speechURL + '" width="350px" height="120px"></iframe>';
    }
    
    function disableVozmeSpeech() {
        speechEnabled = false;
        var speech = document.getElementById("_Speech");
        $('#_Speech').fadeOut('slow', function() {
            speech.innerHTML = '';            
        });
    }

    function toggleVozmeSpeech() {
        if (speechEnabled == true) {
            disableVozmeSpeech();
        }
        else {
            enableVozmeSpeech($('#_Content').text());
        }
    }
    
    function speakSpeech(f) {
        $.getScript("/static/speak/speakClient.js", function(data, textStatus, jqxhr) {
            var content = $('#_Content').text();            
            speak.play(content, {amplitude: 100, wordgap: 5, pitch: 25, speed: 200}, f );
        });
    }

    var stopAutospeech = false;
    function startSpeakAutoSpeech() {
        stopAutospeech = false;
        $('#speaker_icon').attr('src', '/static/icons/sound_playing.png');
        
        if (nextID!=null)
            speakSpeech( function() { if (!stopAutospeech) goNext( function() { if (!stopAutospeech) startSpeakAutoSpeech(); });  } );
        else
            speakSpeech( function() { stopSpeakAutoSpeech();  } );
    }
    function stopSpeakAutoSpeech() {
        stopAutospeech = true;
        $('#speaker_icon').attr('src', '/static/icons/simplistica/sound.png');
        $('#audio').html('');
    }
    
    function goNextExplicit() {
        stopSpeakAutoSpeech();
        goNext();
    }
    function goPreviousExplicit() {
        stopSpeakAutoSpeech();
        goPrevious();
    }

    function renderMainContent(node) {
        var content, title;

        if (node != null) {
            content = node.prop['content'];
            if (content == null)
                content = '';
            if (node.name == null)
                title = '';
            else if (node.name == '')
                title = '';
            else
                title = '<strong>' + node.name + '</strong><br/>';
        }
        else {
            title = '';
            content = "I'm thinking about...";        
        }
        
        var line = '<div id="mainContent">' + title + content + '</div>';
        return line;
    }
    function renderNeighborhood(node) {
        var line = '<table id="neighborTable"><tr>';
        if (node.ins.length > 0) {
            line = line + '<td width="30%" style="vertical-align:top">';
            for (var ii in node.ins) {
                var xi = node.ins[ii];
                var id = xi.id;
                var name = xi.name;
                
                if (name == null) name = xi.preview;
                if (name == null) name = xi.id;

                var relationship = '';
                if (xi.via!=null) {
                    relationship = xi.via.name;
                }
                var r = '<p class="neighborhooditem neighbor_summary_in"><a href="/node/' + id +'">' + name + '</a> -> ' + relationship + '</p>';
                line = line + r;
            }
            line = line + '</td>';
        }

        var numProperties = 0;
        for (var id in node.prop) {
            if (id == 'content') continue;
            if (id == 'name') continue;
            if (id == 'id') continue;
            numProperties++;
        }
        
        if (numProperties > 0) {
            line = line + '<td width="30%" style="vertical-align:top">';
            for (var id in node.prop) {   
                if (id == 'content') continue;
                if (id == 'name') continue;
                if (id == 'id') continue;
                
                var value = node.prop[id];
                var r = '<p class="neighborhooditem neighbor_summary_prop"><a href="/property/' + id +'">' + id + '</a>: ' + value + '</p>';
                line = line + r;
            }
            line = line + '</td>';
        }
        if (node.outs.length > 0) {
            line = line + '<td width="30%" style="vertical-align:top">';
            for (var ii in node.outs) {
                var xi = node.outs[ii];
                var id = xi.id;
                var name = xi.name;
                
                if (name == null) name = xi.preview;
                if (name == null) name = xi.id;
                
                var relationship = '';
                if (xi.via!=null) {
                    relationship = xi.via.name;
                }
                var r = '<p class="neighborhooditem neighbor_summary_out">' + relationship + ' -> <a href="/node/' + id +'">' + name + '</a></p>';
                line = line + r;
            }
            line = line + '</td>';
        }
        line = line + '</tr></table>';
        return line;
        
    }
    
    function showNode(f) {
        
        disableVozmeSpeech();

        currentNode = nodes[f];
        
        var content = document.getElementById("_Content");
        content.innerHTML = renderMainContent(currentNode);

        $('#_Content').attr('contentEditable', widgets['Edit']);

        if (currentNode!=null) {
            
            if (widgets['Neighborhood']) { 
                $("#Neighborhood").html( renderNeighborhood(currentNode) );
                highlightButton('NeighborhoodButton', true);
                $("#Neighborhood").show();
            }
            else {
                highlightButton('NeighborhoodButton', false);
                $("#Neighborhood").hide();
            }
            
            
            var prev = document.getElementById("_Prev");
            if (f == 0) {
                prev.innerHTML = '&nbsp;';
            }
            else {
                prev.innerHTML = '<a href="javascript:goPreviousExplicit()"><img src="/static/icons/left.png" height="32px" width="32px"/></a>';
            }

            var next = document.getElementById("_Next");
            if (f == nodes.length-1) {
                next.innerHTML = '&nbsp;';
            }
            else {
                next.innerHTML = '<a href="javascript:goNextExplicit()"><img src="/static/icons/right.png" height="32px" width="32px"/></a>';
            }

            prevID = nextID = null;

            for (var ii in currentNode.ins) {
                var xi = currentNode.ins[ii];
                var id = xi.id;
                var relationship = '';
                if (xi.via!=null) {
                    relationship = xi.via.name;
                }
                if (relationship == 'next')
                    prevID = id;
            }
            for (var ii in currentNode.outs) {
                var xi = currentNode.outs[ii];
                var id = xi.id;
                var relationship = '';
                if (xi.via!=null) {
                    relationship = xi.via.name;
                }
                if (relationship == 'next')
                    nextID = id;
            }
            var status = document.getElementById("Status");
            if ((prevID!=null) || (nextID!=null))
                status.innerHTML = ((prevID!=null) ? "<--" : "") + " | " + ((nextID!=null) ? "-->" : "");
            else
                status.innerHTML = '';
        }
        
        if (widgets["Edit"]) {
            $('#Edit').show();
            highlightButton('EditButton', true);
        }
        else {
            $('#Edit').hide();                
            highlightButton('EditButton', false);
        }

        
        updateFonts();
        
        $("#_Content").css({opacity: 1.0});

        saveWidgets();
    }

    function goPrevious() {
        //TODO update through AJAX to avoid reloading entire page
        if (prevID!=null)
           setNode(prevID, null);

    }
    
    function goNext() {
        goNext(null);
    }

    function goNext(f) {
        //TODO update through AJAX to avoid reloading entire page
        if (nextID!=null)
           setNode(nextID, f);
       else
           stopAutospeech();
    }
    
    function setNode(id, f) {
        $("#_Content").css({opacity: 0});
        $.getJSON('/node/' + id + '/json', function(data) {
            nodes = []
            _n(data);
            window.history.pushState(id, '', '/node/' + id);
            showNode(0);
            
            if (f!=null)
                f();
        });    
        
    }

    function highlightButton(i, highlighted) {
        var x = $('#' + i);
        if (highlighted)
            x.addClass('MenuButtonHighlighted');
        else
            x.removeClass('MenuButtonHighlighted');
    }

    function updateFont(c) {
        if (c == null)
            return;
        
        c.style.fontSize = fontSize + "px"; 
        var e = c.getElementsByTagName("a");
        for (var i = 0; i < e.length; i++) {
            e[i].style.fontSize = c.style.fontSize;
        }        
        
    }
    
    function updateFonts() {
        //updateFont( document.getElementById("_Content") );
        updateFont( document.getElementById("mainContent") );       
        updateFont( document.getElementById("_GoInput") );  //TODO this is a hack, use JQuery selector for all input-box classes. see go.html
    }

    function fontLarger() {
        fontSize+=5;
        
        updateFonts();
    }

    function fontSmaller() {
        fontSize-=5;
        if (fontSize < 1) fontSize = 1;

        updateFonts();
    }

    function _n(content) {
        nodes.push(content);
    }

    function onFrameSpin(e) {
        var nDelta = 0;
        if (!e) { // For IE, access the global (window) event object
            e = window.event;
        }
        // cross-bowser handling of eventdata to boil-down delta (+1 or -1)
        if ( e.wheelDelta ) { // IE and Opera
            nDelta= e.wheelDelta;
            if ( window.opera ) {  // Opera has the values reversed
                nDelta= -nDelta;
            }
        }
        else if (e.detail) { // Mozilla FireFox
            nDelta= -e.detail;
        }

        if (nDelta < 0) {
            //HandleMouseSpin( 1, e.clientX, e.clientY );
            goPreviousExplicit();
        }
        if (nDelta > 0) {
            //HandleMouseSpin( -1, e.clientX, e.clientY );
            goNextExplicit();
        }

        if ( e.preventDefault ) {  // Mozilla FireFox
            e.preventDefault();
        }
        e.returnValue = false;  // cancel default action
    }
    

    //TODO find a way to combine with previous function
    function onFontSpin(e) {
        var nDelta = 0;
        if (!e) { // For IE, access the global (window) event object
            e = window.event;
        }
        // cross-bowser handling of eventdata to boil-down delta (+1 or -1)
        if ( e.wheelDelta ) { // IE and Opera
            nDelta= e.wheelDelta;
            if ( window.opera ) {  // Opera has the values reversed
                nDelta= -nDelta;
            }
        }
        else if (e.detail) { // Mozilla FireFox
            nDelta= -e.detail;
        }
        if (nDelta > 0) {
            //HandleMouseSpin( 1, e.clientX, e.clientY );
            fontLarger();
        }
        if (nDelta < 0) {
            //HandleMouseSpin( -1, e.clientX, e.clientY );
            fontSmaller();
        }
        if ( e.preventDefault ) {  // Mozilla FireFox
            e.preventDefault();
        }
        e.returnValue = false;  // cancel default action
    }

    function setup() {
        var panel = document.getElementById("_Panel");
        var control = document.getElementById("_Control");
        var content = document.getElementById("_Content");
        var frameSpin = document.getElementById("Status");
        var font = document.getElementById("_Font");

        if (frameSpin.addEventListener) {
            frameSpin.addEventListener('DOMMouseScroll', onFrameSpin, false);
            frameSpin.addEventListener('mousewheel', onFrameSpin, false); // Chrome
        }
        else {
            frameSpin.onmousewheel = onFrameSpin;
        }

        if (font.addEventListener) {
            font.addEventListener('DOMMouseScroll', onFontSpin, false);
            font.addEventListener('mousewheel', onFontSpin, false); // Chrome
        }
        else {
            font.onmousewheel= onFontSpin;
        }

        var w = $.totalStorage('widgets');
        if (w != undefined) {
            widgets = w;
        }
        
        
    }

    function enlargeImage(element, imagesrc) {
        element.innerHTML = '<img src=\"' + imagesrc + '\"/>';
    }
    

    function graphIt() {
        newWindowIFrame('Neighborhood Graph', '/graph/' + currentNode['id']);
    }
    
    function imageIt() {
        //TODO filter 'q' for useless prepositions like 'the', 'and', etc
        var selection = selectedText;
        if (selection == '') {
            alert('Select some text with which to find images.');
            return;
        }

        //images.search.yahoo.com/search/images?p=test

        var iurl = 'http://images.search.yahoo.com/search/images?p=' + escape(selection);
        
        newWindowIFrame('Image results for: ' + selection, iurl);
        
    }

    var eid = 0;
    function newWindow(theTitle, x) {
        var newID = ("Window" + eid);
        eid++;        
        $('#Window').append( "<div id='" + newID + "'>" + x + "</div>" );

        var w = $("body").find('#' + newID);
        
//        $('#' + newID).dialog({title: theTitle, width: '60%', height: 450} );
//        $('#' + newID).fadeIn();
        w.dialog({title: theTitle, width: '60%', height: 450} );
        w.fadeIn();

    }
    function newWindowIFrame(theTitle, url) {
        newWindow(theTitle, '<iframe src=\"' + url + '\" width="98%" height="98%"></iframe>');
    }
    

    function setTheme(theme) {       
        currentTheme = theme;

        var c = document.getElementById("themeCSS");
        c.href = '/static/themes/' + theme + '.css';
        localStorage['theme'] = theme;
    }
    

    //Setup escape-key events
    document.onkeydown = function(e){
        var keycode;
        if (e == null) { // ie
            keycode = event.keyCode;
        } else { // mozilla
            keycode = e.which;
        }
        
        if (!widgets['Edit']) {
            
            if (keycode == 37) {
                //left
                goPreviousExplicit();
            }
            else if (keycode == 38) {
                //up
                fontLarger();
            }
            else if (keycode == 39) {
                //right
                goNextExplicit();
            }
            else if (keycode == 40) {
                //down
                fontSmaller();
            }
        }
    };

    function onContentMouseOver(e) {
    }
    function onContentMouseOut(e) {
        e.className='';
    }
    
//    function setOriginal(o) {
//        pageurl = o;
//    }
    
        
//    function showHelp() {
//        $( "#dialog-message" ).dialog({
//                width: '75%',
//                modal: true,
//                buttons: {
//                        Ok: function() {
//                                $( this ).dialog( "close" );
//                        }
//                }
//        });        
//    }
//    <div id="dialog-message" title="About Cortexit" style="display:none">
//        <center><iframe src="/static/cortexit/about.html" width="100%" height="400px"></iframe></center>
//    </div>


    function saveContent() {
        currentNode.prop['content'] = $('#_Content').html();
    }
    
    function ensureContentSaved() {
        if (confirm("Save edits?")) { 
            saveContent();
        }

    }
    
    var contentBeforeEdit = '';
    function setEditable(e) {
        widgets['Edit'] = e;
        
        if (!e) {
            if (contentBeforeEdit != $('#_Content').html())
                ensureContentSaved();
        }
        else {
        }
        contentBeforeEdit = $('#_Content').html();
        
        showNode(0);
    }
    
    function toggleEdit() {
        setEditable(!widgets['Edit']);        
    }
    function toggleNeighborhood() {
        widgets['Neighborhood'] = !widgets['Neighborhood'];
        showNode(0);
    }


    function shareIt() {
        $('#atbutton').css('display', 'inline');
        //var c = cframes[currentFrame];
        var c = $('#_Content').text();
        
        
        var tbx = document.getElementById("attb");
        var svcs = {facebook: 'Facebook', twitter: 'Twitter', blogger: 'Blogger', reddit: 'Reddit', email: 'Email', print: 'Print', googletranslate: 'Translate', expanded: 'More'};

        tbx.innerHTML = '';
        for (var s in svcs) {
            tbx.innerHTML += '<a class="addthis_button_'+s+' addthis_32x32_style">'+svcs[s]+'</a>';
        }
        
        var addthis_share = 
        { 
            templates: {
                           twitter: '{{title}} {{url}}'
                       }
        };
                
        addthis.toolbox("#attb", addthis_share , {url: pageurl, title: c, description: c});
        addthis.button("#atlink", addthis_share , {url: pageurl, title: c, description: c});
        
        $('#attbtext').html( '<b>"' + c + '"</b><br/>' + pageurl + '<hr/>' );
        
        $( "#share-modal" ).dialog({
                width: screen.width * 0.75,
                height: screen.height * 0.75,
                modal: true
        });
        
    }

//setup theme
var currentTheme = localStorage['theme'];
if (currentTheme == null) {
    currentTheme = 'default-black';
}
setTheme(currentTheme);

$(document).ready(function(){
      $('#_Speech').fadeToggle();    
      
      jQuery('ul.sf-menu').superfish();

});

<!-- Original:  Ronnie T. Moore Web Site:  The JavaScript Source -->
<!-- This script and many more are available free online at The JavaScript Source!! http://javascript.internet.com -->

var selectedText = "";
function getActiveText(e) { 
    // Sets text MSIE or Netscape active text based on browser, puts text in form
    selectedText = (document.all) ? document.selection.createRange().text : document.getSelection();
    return true;
}

document.onmouseup = getActiveText;
if (!document.all) document.captureEvents(Event.MOUSEUP);
