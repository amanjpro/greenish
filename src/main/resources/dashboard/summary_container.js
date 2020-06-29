const e = React.createElement;

class SummaryContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      items: []
    };
  }

  componentDidMount() {
    fetch("/summary")
      .then(res => res.json())
      .then(
        (items) => {
          this.setState({
            isLoaded: true,
            items: items
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
    const { error, isLoaded, items } = this.state;
    if (error) {
      return React.createElement('div', null, `Error: ${error.message}`);
    } else if (!isLoaded) {
      return React.createElement('div', null, 'Loading...');
    } else {
      return (
        React.createElement('ul', null,
          items.map(item => (
            React.createElement('li',
              {key: `${item.name}`},
              `${item.name} ${item.status}`)
          )))
      );
    }
  }
}

const domContainer = document.querySelector('#summary_container');
ReactDOM.render(e(SummaryContainer), domContainer);
