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
        <div>
          <h3 key="state_header">All data sets&nbsp;
            <sub className="link" onClick={() => this.setState({page:"main"})}>See main dashboard</sub>
          </h3>
          <StateContainer endpoint='state'/>
        </div>
      )
    } else if(page == 'group'){
      return(<GroupContainer group={gid} handler={this.handler}/>)
    } else if(page == 'job'){
      return(<JobContainer group={gid} job={jid} handler={this.handler}/>)
    } else { // page == 'main'
      return(
        <div>
          <SummaryContainer handler={this.handler}/>
          <h3 key="state_header">Detailed missing periods&nbsp;
            <sub className="link" onClick={() => this.setState({page:"state"})}>See all periods</sub>
          </h3>
          <StateContainer endpoint='missing'/>
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
        <h1 key="greenish_dashboard_header">Greenish dashboard</h1>
        {this.renderMain(this.state.page, this.state.gid, this.state.jid)}
      </div>
    )
  }
}

const domContainer = document.querySelector('#main_container');
ReactDOM.render(e(MainContainer), domContainer);
