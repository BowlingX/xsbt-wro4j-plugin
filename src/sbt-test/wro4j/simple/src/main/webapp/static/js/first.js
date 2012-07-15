/*
 * Copyright (c) 2012 David Heidrich
 * A Simple Popup Widget for jQuery UI
 */
(function ($) {
    $.widget("ui.popup", {
        version:"1.0",
        /**
         * Default Optionen
         */
        options:{
            name: "popup",
            width: 600,
            height: 400,
            resizable: false
        },
        /**
         * Initialisiert das Widget
         */
        _create:function () {
            var self = this;
            $(this.element).click(function(e){
                e.preventDefault();
                var left = (screen.width/2)-(self.options.width/2);
                var top = (screen.height/2)-(self.options.height/2);
                var options = "width="+self.options.width+"," +
                    "height="+self.options.height+",resizable="+ (self.options.resizable? "'yes'" : "'no'"+",top="
                    +top+",left="+left);
                window.open($(this).attr("href"), self.options.name , options);
            });
        },
        /**
         * Entfernt das Widget
         */
        destroy:function () {
            $.Widget.prototype.destroy.apply(this, arguments);
        }

    })
})(jQuery);


