class StateContainer extends React.Component {
  intervalID
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      groups: [],
      endpoint: props.endpoint,
    };
  }

  componentDidMount() {
    this.fetchData()
  }

  componentWillUnmount() {
    clearTimeout(this.intervalID);
  }

  fetchData = () => {
    fetch(`/${this.state.endpoint}`)
      .then(res => res.json())
      .then(
        (groups) => {
          this.setState({
            isLoaded: true,
            groups: groups
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
    this.intervalID = setTimeout(this.fetchData, fetchInterval);
  }

  render() {
    const { error, isLoaded, groups, endpoint} = this.state;
    if (error) {
      return (
        <div>Error: {error.message}</div>
      )
    } else if (!isLoaded) {
      return (
        <div>Loading...</div>
      )
    } else {
      return (
        <div key={`${endpoint}-div-grid`} className='grid-container-detail'>
          {renderState(groups, endpoint, 'grid-item')}
        </div>
      )
    }
  }
}
