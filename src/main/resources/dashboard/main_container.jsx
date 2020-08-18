const e = React.createElement;
const Link = ReactRouterDOM.Link;

class MainContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      page: props.page,
      gid: props.group,
      jid: props.job
    }
    this.handler = this.handler.bind(this);
    this.renderMain = this.renderMain.bind(this);
  }

  renderMain(page, gid, jid, handler) {
    if (page == 'state') {
      return (
        <div className='detail-div'>
          <h2 key="state_header">All data sets&nbsp;
            <sub>
              <Link to={loc => `${loc.pathname}?page=main`}
                  onClick={() => this.setState({page:"main"})} className="link">
                See main dashboard
              </Link>
            </sub>
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
              <sub>
                <Link to={loc => `${loc.pathname}?page=state`}
                    onClick={() => this.setState({page:"state"})} className="link">
                  See all periods
                </Link>
              </sub>
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
        <div className='header-div'>
          <div className='header-left'>
            <h1 key="greenish_dashboard_header" className='greenish-header'>
              <img src="greenish-logo.svg" width="100" height="100"/>
              <NamespaceContainer/>
            </h1>
          </div>
          <div className='header-right'>
            <div className='time-div'><TimeContainer/></div>
            <div className='version-div'><VersionContainer/></div>
          </div>
        </div>
        <div>
          {this.renderMain(this.state.page, this.state.gid, this.state.jid)}
        </div>
      </div>
    )
  }
}

const domContainer = document.querySelector('#main_container');
const BrowserRouter = ReactRouterDOM.BrowserRouter;
const Route = ReactRouterDOM.Route;
const useLocation = ReactRouterDOM.useLocation;

function useQuery() {
  return new URLSearchParams(useLocation().search);
}

function ShowPage() {
  let query = useQuery();
  let page = query.get("page");
  let gid = query.get("gid");
  let jid = query.get("jid");
  return (<MainContainer page={page} group={gid} job={jid}/>);
}
ReactDOM.render(
  <BrowserRouter><Route><ShowPage/></Route></BrowserRouter>,
  domContainer
);
