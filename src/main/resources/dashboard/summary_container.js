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
      return (
        <div>Error: {error.message}</div>
      )
    } else if (!isLoaded) {
      return (
        <div>Loading...</div>
      )
    } else {
      return (
        <div key='GridDiv' className='grid-container'>
          {items.map((group, gid) => (
            <div key={`group-${gid}`} className='grid-item'>
              <h3 key={`group-${gid}-header`}>{group.name}</h3>
              <table key={`group-${gid}-table`}>
                <tbody key={`group-${gid}-tbody`}>
                  <tr key={`group-${gid}-tr-header`}>
                    <th key={`group-${gid}-th-1`}>Job Name</th>
                    <th key={`group-${gid}-th-2`}># Missing Data Sets</th>
                  </tr>
                  {group["status"].map((job, jid) =>(
                    <tr key={`job-${gid}-${jid}-row`} className={job.alert_level}>
                      <td key={`job-${gid}-${jid}-name`}>{job.name}</td>
                      <td key={`job-${gid}-${jid}-missing`}>{job.missing}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )
    }
  }
}

const domContainer = document.querySelector('#summary_container');
ReactDOM.render(e(SummaryContainer), domContainer);
