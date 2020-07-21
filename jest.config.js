const {defaults} = require('jest-config');

module.exports = {
  verbose: true,
  transform: {
    "^.+\\.jsx?$": "babel-jest"
  },
  roots: [
    "<rootDir>",
    "<rootDir>/src/test/js",
    "<rootDir>/target/scala-2.13/resource_managed/main/dashboard"
  ],
  moduleDirectories: [
    "node_modules",
  ],
  "moduleNameMapper": {
     "^./(.*)_container.js$": "<rootDir>/target/scala-2.13/resource_managed/main/dashboard/$1_container.js",
     "^./resources/(.*).js$": "<rootDir>/json-samples/$1.js"
  },
  "setupFilesAfterEnv": [
    "<rootDir>/jest.setup.js"
  ]
};
