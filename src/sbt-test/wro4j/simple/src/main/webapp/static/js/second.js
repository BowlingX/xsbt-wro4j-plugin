/*
 * Copyright (c) 2012 David Heidrich
 * A Translation Helper for jQuery, supports arguments
 */
_ = function(string) {

    if("object" == typeof TRANSLATION) {
        var msg =  TRANSLATION.messages[string] || string;
        if(arguments.length > 1) {
            var params =  Array.prototype.slice.call(arguments);
            params.shift();

            $.each(params, function(i){
                msg = msg.replace("{"+i+"}", encode(params[i]));
            });
        }
        // Return an escaped string:
        return msg;

    }
};

encode = function(s) {
    return $('<div/>').text(s).html();
};
