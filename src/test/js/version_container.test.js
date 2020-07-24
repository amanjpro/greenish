import React from 'react';

import { shallow } from 'enzyme';

import VersionContainer from './version_container.js';

import { testObject } from './resources/system.js';

describe('VersionContainer', () => {
  it('fetches version from server when server returns a successful response', done => {
    const mockSuccessResponse = {};
    const mockJsonPromise = Promise.resolve(testObject);
    const mockFetchPromise = Promise.resolve({
      json: () => mockJsonPromise,
    });
    global.fetch = jest.fn().mockImplementation(() => mockFetchPromise);

    const wrapper = shallow(<VersionContainer/>);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith('/system');

    process.nextTick(() => {
      expect(wrapper.state()).toEqual({
        "error": null,
        "isLoaded": true,
        "version": "1.4.0-SNAPSHOT"
      });

      global.fetch.mockClear();
      delete global.fetch;
      done();
    });
  });
  it('shows error, when a bad json is returned from API', done => {
    const mockSuccessResponse = {};
    const mockJsonPromise = Promise.reject({"nah": "bad"});
    const mockFetchPromise = Promise.resolve({
      json: () => mockJsonPromise,
    });
    global.fetch = jest.fn().mockImplementation(() => mockFetchPromise);

    const wrapper = shallow(<VersionContainer/>);

    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith('/system');

    process.nextTick(() => {
      expect(wrapper.state()).toEqual({
        "error": {"nah": "bad"},
        "isLoaded": true,
        "version": null,
      });

      global.fetch.mockClear();
      delete global.fetch;
      done();
    });
  });
});
