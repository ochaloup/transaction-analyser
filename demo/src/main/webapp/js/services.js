angular.module('demoService', ['ngResource']).
    factory('Demo', function ($resource) {
        return $resource('rest/demos/:demoId', {});
    });
angular.module('demoAutoService', ['ngResource']).
factory('DemoAuto', function ($resource) {
    return $resource('rest/demo_auto/:act', {});
});