class NamespaceContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      namespace: null
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
          if('namespace' in info) {
            this.setState({
              isLoaded: true,
              namespace: info.namespace
            });
          } else {
            this.setState({
              isLoaded: true
            });
          }
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
    const { error, isLoaded, namespace} = this.state;
    if (error) {
      return (<span> - Error: {error.message}</span>)
    } else if (!isLoaded) {
      return (<span> - Loading...</span>)
    } else {
      return (namespace != null?<span> - {namespace}</span>:null)
    }
  }
}
