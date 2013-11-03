define([
    'angular-mocks',
    'js/directives/cpmrMenu',
    'js/services/menu'
  ],

  function() {
  
    'use strict';

    describe('cpmrMenu directive spec', function() {

      var $scope, $compile;

      beforeEach(module('cpmr.directives'));

      beforeEach(inject(function(_$rootScope_, _$compile_) {
        $scope = _$rootScope_;
        $compile = _$compile_;
      }));

      var compileMenu = function (markup, scope) {
        var el = $compile(markup)(scope);
        scope.$digest();
        return el;
      };

      it('should set the current page\'s menu class to \'active\'', function() {

        $scope.menu = [{
            'active': false,
            'link': 'test.list#edit',
            'text': 'Menu Item #1'
          },
          {
            'active': true,
            'link': 'test.list',
            'text': 'Menu Item #2'
          }
        ];

        var menu = compileMenu('<cpmr-menu menu=\'menu\'></cpmr-menu>', $scope);

        var activeLink = menu.find('li.active a');
        expect(activeLink.text()).toBe('Menu Item #2');

        var inactiveLink = menu.find('li:not(.active) a');
        expect(inactiveLink.text()).toBe('Menu Item #1');
      });
    });
  }
);