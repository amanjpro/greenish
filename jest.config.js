const {defaults} = require('jest-config');

module.exports = {
  verbose: true,
  collectCoverage: true,
  coverageReporters: ["json", "html"],
  transform: {
    "^.+\\.jsx?$": "babel-jest"
  },
  roots: [
    "<rootDir>",
     "<rootDir>src/main/resources/dashboard"
  ],
  moduleDirectories: [
    "node_modules",
  ],
  "moduleNameMapper": {
     "^./(.*)_container.js$": "<rootDir>src/main/resources/dashboard/$1_container.jsx",
     "^./resources/(.*).js$": "<rootDir>/src/test/resources/json-samples/$1.js"
  },
  "setupFilesAfterEnv": [
    "<rootDir>/jest.setup.js"
  ]
};
