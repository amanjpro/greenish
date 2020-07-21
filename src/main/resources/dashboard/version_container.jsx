import React from 'react';

class VersionContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      version: null
    };
  }

  componentDidMount() {
    this.fetchData()
  }

  fetchData = () => {
    fetch(`/system`)
      .then(res => res.json())
      .then(
        (info) => {
          this.setState({
            isLoaded: true,
            version: info.version
          });
        },
        // Note: it's important to handle errors here
        // instead of a catch() block so that we don't swallow
        // exceptions from actual bugs in components.
        (error) => {
          this.setState({
            isLoaded: true,
            error
          });
        }
      )
  }

  render() {
    const { error, isLoaded, version} = this.state;
    if (error) {
      return (
        <em>Error: {error.message}</em>
      )
    } else if (!isLoaded) {
      return (
        <em>Loading...</em>
      )
    } else {
      return (
        <em>Version {version}</em>
      )
    }
  }
}
export default VersionContainer;
