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
	$scope.value = 1;
	$scope.min = 1;
	$scope.max = 50;
    $scope.perform = function () {
        DemoAuto.get({noTrans : $scope.value}, function (result) {});
    };
}