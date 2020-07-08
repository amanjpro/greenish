const e = React.createElement;

class MainContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: 'main',
      gid: null,
      jid: null
    }
    this.handler = this.handler.bind(this);
    this.renderMain = this.renderMain.bind(this);
  }

  renderMain(page, gid, jid, handler) {
    if (page == 'state') {
      return (
        <div className='detail-div'>
          <h2 key="state_header">All data sets&nbsp;
            <sub className="link" onClick={() => this.setState({page:"main"})}>See main dashboard</sub>
          </h2>
          <StateContainer endpoint='state'/>
        </div>
      )
    } else if(page == 'group'){
      return(
        <div className="detail-div">
          <GroupContainer group={gid} handler={this.handler}/>
        </div>
      )
    } else if(page == 'job'){
      return(
        <div className="detail-div">
          <JobContainer group={gid} job={jid} handler={this.handler}/>
        </div>
      )
    } else { // page == 'main'
      return(
        <div>
          <div className='summary-div'>
            <h2 key="state_header">Summary</h2>
            <SummaryContainer handler={this.handler}/>
          <br/><br/>
          </div>
          <div className="detail-div">
            <h2 key="state_header">Detailed missing periods&nbsp;
              <sub className="link" onClick={() => this.setState({page:"state"})}>See all periods</sub>
            </h2>
            <StateContainer endpoint='missing'/>
          </div>
        </div>
      )
    }
  }

  handler(page, gid, jid) {
    this.setState({
      page: page,
      gid: gid,
      jid: jid,
    })
  }

  render() {
    return (
      <div>
        <h1 key="greenish_dashboard_header" className='greenish-header'>Greenish dashboard</h1>
        {this.renderMain(this.state.page, this.state.gid, this.state.jid)}
      </div>
    )
  }
}

const domContainer = document.querySelector('#main_container');
ReactDOM.render(e(MainContainer), domContainer);
