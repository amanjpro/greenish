const e = React.createElement;

function timestampToDate(timestamp) {
  return new Intl.DateTimeFormat('en-GB', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'}).format(timestamp);
}

function renderState(groups, subClassName, keyPrefix) {
  return (
    groups.map(groupStatus => {
      const group = groupStatus.group;
      const gid = group.group_id;
      return (<div key={`${keyPrefix}-group-${gid}`} className={subClassName}>
        <h3 key={`${keyPrefix}-group-${gid}-header`}>{group.name}</h3>
        <table key={`${keyPrefix}-group-${gid}-table`}>
          <tbody key={`${keyPrefix}-group-${gid}-tbody`}>
            <tr key={`${keyPrefix}-group-${gid}-tr-header`}>
              <th key={`${keyPrefix}-group-${gid}-th-1`}>Job name</th>
              <th key={`${keyPrefix}-group-${gid}-th-2`}>Data set period</th>
              <th key={`${keyPrefix}-group-${gid}-th-3`}>Last updated</th>
            </tr>
            {
              groupStatus["status"].map(jobStatus =>{
                if(jobStatus["period_health"] != undefined) {
                  const job = jobStatus.job;
                  const jid = job.job_id;
                  const date = timestampToDate(jobStatus.updated_at);
                  return(jobStatus.period_health.map((ph, i) => (
                    <tr key={`${keyPrefix}-job-${gid}-${jid}-${i}-row`}>
                      <td key={`${keyPrefix}-job-${gid}-${jid}-${i}-job`}>{job.name}</td>
                      <td key={`${keyPrefix}-job-${gid}-${jid}-${i}-period`}>{ph.period}</td>
                      <td key={`${keyPrefix}-job-${gid}-${jid}-${i}-updated`}>{date}</td>
                    </tr>
                  )))
                }
              })
            }
          </tbody>
        </table>
      </div>)
    })
  );
}

class MissingContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      groups: []
    };
  }

  componentDidMount() {
    fetch("/missing")
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
  }

  render() {
    const { error, isLoaded, groups } = this.state;
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
        <div key='missing-div-grid' className='grid-container'>
          {renderState(groups, 'missing', 'grid-item')}
        </div>
      )
    }
  }
}

const domContainer = document.querySelector('#missing_container');
ReactDOM.render(e(MissingContainer), domContainer);
