import React from 'react';

class TimeContainer extends React.Component {
  intervalID
  constructor(props) {
    super(props);
    this.state = {
      now: ""
    };
  }

  componentDidMount() {
    this.refresh()
  }

  componentWillUnmount() {
    clearTimeout(this.intervalID);
  }

  refresh = () => {
    const d = new Date()
    this.setState({now: d.toUTCString()})
    this.intervalID = setTimeout(this.refresh, fetchInterval);
  }

  render() {
    const { now } = this.state;
    return (
      <em key={`utc-time-div`}>
        {now}
      </em>
    )
  }
}

export default VersionContainer;
