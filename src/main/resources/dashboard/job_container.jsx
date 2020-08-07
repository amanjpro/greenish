const Link = ReactRouterDOM.Link;

class JobContainer extends React.Component {
  intervalID
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      job: null
    };
    this.handleBack = this.handleBack.bind(this);
  }

  componentDidMount() {
    this.fetchData()
  }

  componentWillUnmount() {
    clearTimeout(this.intervalID);
  }

  fetchData = () => {
    fetch(`/group/${this.props.group}/job/${this.props.job}`)
      .then(res => res.json())
      .then(
        (job) => {
          this.setState({
            isLoaded: true,
            job: job
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

  handleBack() {
    this.props.handler("main", null, null);
  }

  render() {
    const { error, isLoaded, job } = this.state;
    if (error) {
      return (
        <div>Error: {error.message}</div>
      )
    } else if (!isLoaded) {
      return (
        <div>Loading...</div>
      )
    } else {
      const jobs = renderJob(job, this.props.group, 'job-view')
      return (
        <div key='job-div-view' className='detail-box'>
          <h2 key={`job-view-${this.props.group}-${this.props.job}-header`}>
            {job.job.name}&nbsp;
            <sub>
              <Link to={loc => `${loc.pathname}?page=main`}
                  onClick={this.handleBack}
                  className="link">
                See main dashboard
              </Link>
            </sub>
          </h2>
          {"owner" in job.job?<div className="owner-div">Owned by: {job.job.owner}</div>:<div/>}
          <div className="stdout-div">
            <a href={`/group/${this.props.group}/job/${this.props.job}/stdout`}
              className="link">stdout</a>
          </div>
          {encloseInTable(jobs, 'job-view', this.props.group)}
        </div>
      )
    }
  }
}
