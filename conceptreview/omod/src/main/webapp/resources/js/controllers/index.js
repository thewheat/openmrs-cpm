define([
    'angular',
    'js/services/index'
  ],
  function (angular) {

    'use strict';
    
    return angular.module('conceptreview.controllers', ['ngSanitize', 'conceptreview.services']);
  }
);