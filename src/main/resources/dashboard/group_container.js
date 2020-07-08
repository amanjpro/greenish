class GroupContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      gid: props.group,
      group: null
    };
    this.handleBack = this.handleBack.bind(this);
  }

  componentDidMount() {
    fetch(`/group/${this.state.gid}`)
      .then(res => res.json())
      .then(
        (group) => {
          this.setState({
            isLoaded: true,
            group: group
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

  handleBack() {
    this.props.handler("main", null, null);
  }

  render() {
    const { error, isLoaded, gid, group } = this.state;
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
        <div key='group-div-view'>
          <h5 className="link" onClick={this.handleBack}>See main dashboard</h5>
          {renderGroup(group, 'group-view', 'grid-item')}
        </div>
      )
    }
  }
}
