function DemoCtrl($scope, $http, Demo) {
    $scope.refresh = function () {
        $scope.demos = Demo.query();
    };

    $scope.invoke_demo = function ($id) {
        Demo.get({demoId: $id}, function (result) {
            alert(result.msg);
        });
    };

    $scope.refresh();

    $scope.orderBy = 'id';
}

function DemoAutoCtrl($scope, $http, DemoAuto) {
    $scope.perform = function ($action) {
        DemoAuto.get({act : $action}, function (result) {});
    };
}