class SummaryContainer extends React.Component {
  intervalID
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      items: []
    };
    this.handleGroupClick = this.handleGroupClick.bind(this);
    this.handleJobClick = this.handleJobClick.bind(this);
  }

  componentDidMount() {
    this.fetchData()
  }

  componentWillUnmount() {
    clearTimeout(this.intervalID);
  }

  fetchData = () => {
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
    this.intervalID = setTimeout(this.fetchData, fetchInterval);
  }

  handleGroupClick(gid) {
    this.props.handler("group", gid, null);
  }

  handleJobClick(gid, jid) {
    this.props.handler("job", gid, jid);
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
        <div key='overview-grid' className='grid-container'>
          {
            items.map(group => {
              const gid = group.group_id;
              return (
                <div key={`group-${gid}`} className='grid-item dashboard-box'>
                  <h2 key={`group-${gid}-header`} className='link'
                        onClick={() => {this.handleGroupClick(gid)}}>
                    {group.name}
                  </h2>
                  <table key={`group-${gid}-table`}>
                    <tbody key={`group-${gid}-tbody`}>
                      <tr key={`group-${gid}-tr-header`}>
                        <th key={`group-${gid}-th-1`}>Job name</th>
                        <th key={`group-${gid}-th-2`}># Missing data sets</th>
                      </tr>
                      {
                        group["status"].map(job =>{
                          const jid = job.job_id;
                          return(
                            <tr key={`job-${gid}-${jid}-row`}>
                              <td key={`job-${gid}-${jid}-name`}
                                  className='link'
                                  onClick={() => {this.handleJobClick(gid, jid)}}>
                                {job.name}
                              </td>
                              <td className={job.alert_level}
                                key={`job-${gid}-${jid}-missing`}>{job.missing}</td>
                            </tr>
                          )
                        }
                      )}
                    </tbody>
                  </table>
                </div>
              )
            })
          }
        </div>
      )
    }
  }
}
