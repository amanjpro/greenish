const e = React.createElement;

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
        <div className='header-div'>
          <h1 key="greenish_dashboard_header" className='greenish-header'>
            Greenish dashboard<NamespaceContainer/>
          </h1>
          <div className='time-div'><TimeContainer/></div>
          <div className='version-div'><VersionContainer/></div>
        </div>
        <div>
          {this.renderMain(this.state.page, this.state.gid, this.state.jid)}
        </div>
      </div>
    )
  }
}

const domContainer = document.querySelector('#main_container');
// const BrowserRouter = require("react-router-dom").BrowserRouter;
// const Route = require("react-router-dom").Route;
// const Link = require("react-router-dom").Link;
//ReactDOM.render(e(MainContainer), domContainer);
const BrowserRouter = window.ReactRouterDOM.BrowserRouter;
const Route = window.ReactRouterDOM.Route;
const Switch = window.ReactRouterDOM.Switch;
const useParams = window.ReactRouterDOM.useParams;
const useLocation = window.ReactRouterDOM.useLocation;

function Group() {
  let { gid } = useParams();
  console.log(gid);
  <div><MainContainer page="group" group={gid}/></div>
}
function Job() {
  let { gid, jid } = useParams();
  <MainContainer page="job" group={gid} job={jid}/>
}

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
  // , domContainer
  <BrowserRouter>
    <Route><ShowPage/></Route>
  </BrowserRouter>, domContainer
  //   <Switch>
  //       <Group/>
  //     </Route>
  //     <Route path="/dashboard/#group/:gid/job/:jid">
  //       <Job/>
  //     </Route>
  //     <Route exact path="/dashboard/#state">
  //       <MainContainer page="state"/>
  //     </Route>
  //     <Route>
  //       // <Job/>
  //       <MainContainer page="main"/>
  //     </Route>
  //   </Switch>
  // </BrowserRouter>, domContainer
);
