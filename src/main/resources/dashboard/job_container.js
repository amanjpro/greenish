class JobContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      gid: props.group,
      jid: props.job,
      job: null
    };
  }

  componentDidMount() {
    fetch(`/group/${this.state.gid}/job/${this.state.jid}`)
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
  }

  render() {
    const { error, isLoaded, gid, jid, job } = this.state;
    if (error) {
      return (
        <div>Error: {error.message}</div>
      )
    } else if (!isLoaded) {
      return (
        <div>Loading...</div>
      )
    } else {
      const jobs = renderJob(job, this.state.gid, 'job-view')
      return (
        <div key='job-div-view'>
          <h3 key={`job-view-${gid}-${jid}-header`}>{job.job.name}</h3>
          {encloseInTable(jobs, 'job-view', this.state.gid)}
        </div>
      )
    }
  }
}
