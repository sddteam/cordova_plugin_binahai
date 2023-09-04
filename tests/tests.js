exports.defineAutoTests = function(){
    describe('BinahAi', function(){
        it('should have a method startCamera', function(){
            expect(BinahAi.startCamera).toBeDefined();
        });
    
        it('should return a string when calling startCamera', function(){
            expect(BinahAi.startCamera()).toEqual(jasmine.any(String));
        });
    });
};

exports.defineManualTests = function(contentEl, createActionButton){

};