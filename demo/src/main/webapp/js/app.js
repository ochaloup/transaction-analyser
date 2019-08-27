angular.module('txdemo', ['ngRoute', 'demoService', 'demoAutoService']).config(
    ['$routeProvider', function ($routeProvider) {
        $routeProvider.
            when('/demo', { templateUrl: 'partials/demo.html', controller: 'DemoCtrl' }).
            when('/demo_auto', { templateUrl: 'partials/demo_auto.html', controller: 'DemoAutoCtrl' }).
            otherwise({ redirectTo: '/demo' });
    }]);
